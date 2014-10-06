(ns assistant.core
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:require [cljs-http.client :as http]
            [cljs.reader :as reader]
            [garden.core :refer [css]]
            [cognitect.transit :as t]
            [cljs.core.async :refer [<! >! chan]]
            [hickory.core :as hk]
            [assistant.utils :as utils]
            [hickory.select :as s]
            [om.core :as om :include-macros true]
            [om.dom :as dom :include-macros true]))

;; a list of plugins installed by default
;; user can alwasy disalbe them in the ~/.assistant-plugins

(defn log [m]
  (.log js/console m))

(defn printcol [col]
  (doall (map print col)))

(enable-console-print!)

(def gui (js/require "nw.gui"))

(def fs (js/require "fs"))

(def config-file (str (utils/user-home) "/.assistant"))
(utils/create-if-not-exist config-file)
(def config-data (reader/read-string (.readFileSync fs config-file "utf-8")))

;; return config data
(defn config []
  config-data)

;; enable default menu
(defn create-built-in-menu! []
  (let [win (.get (.-Window gui)) ;; current window
        Menu (.-Menu gui) ;; create menu
        mb (Menu. #js {:type "menubar" })]
    (.createMacBuiltin mb "Assistant")
    (set! (.-menu win) mb)))


(create-built-in-menu!)

(defn read-app-state []
  "read app state from ~/.assistant-store"
  (try
    (let [file-name (str (utils/user-home) "/.assistant-store")
          file-exists (utils/create-if-not-exist file-name)
          file-content (.readFileSync fs file-name "utf-8")
          r (t/reader :json)
          state (t/read r file-content)]
      (print "Restore state now....")
      state) (catch js/Error e {:cards []})))

;; app state might contains entire application's configuration info
(def app-state (atom {:cards []}))

(def cards (atom {}))
(def dispatchers (atom {}))

(def styles (atom []))

(def dispatcher-chan (chan))


(defn put-result [result]
  "Put a result into channel"
  (go (>! dispatcher-chan result)))

(let [win (.get (.-Window gui))
      w (t/writer :json)]
  (.on win "close" #(this-as me (do
                                  (print "Start writing....")
                                  (utils/write-to-file (str (utils/user-home) "/.assistant-store" ) (t/write w @app-state))
                                  (.close me true)))))


(defn handle-change [e owner {:keys [text]}]
  (om/set-state! owner :text (.. e -target -value)))

;; application component will be a something like:
;; a text box which accept the commands or words from people
;; a List of components area/playgroud to show the response of any given commands


;; push card data into app data stack
(defn push-card [app card]
  (om/transact! app :cards (fn [xs] (cons card xs))))


(defn dispatch-input [result-chan text]
  (let [parts (.split text " ")
        command-name (keyword (first parts))
        query (clojure.string/join " "(rest parts))
        dispatcher (get-in @dispatchers [command-name :exec])]
    (if dispatcher
      ;; using correspond dispatcher/processor to process the text
      (dispatcher result-chan query)
      ;; not found dispatcher, show a warn card
      (put-result {:type :info-card :info-type "warn" :title (str "Unknown Command - " text) :content "You can run command 'help' to get all available commands."}))))

(defn empty-card []
  (dom/div #js {:className "empty-card"} "Design is not just what it looks like and feels like. Design is how it works. -- Steve Jobs"))


;; general card view om componnet
(defn card-view-om [data owner]
  (reify
    om/IRender
    (render [this]
      (let [card-type (:type data)
            card-fn (get @cards card-type)]
        (when card-fn
          (dom/li #js {:className (str "card " (name card-type))} (om/build card-fn data)))))))


(defn card-view [data owner]
  (let [card-type (:type data)
        card-fn   (get @cards card-type)]
    (when card-fn
      (dom/li #js {:className (str "card " (name card-type))} (om/build card-fn data)))))


(defn app-view [app owner]
  (reify
    om/IInitState
    (init-state [_]
      {:count 1 :text ""})

    om/IWillMount
    (will-mount [_]
      (go (loop []
            (let [result (<! dispatcher-chan)]
              (push-card app result)
              (recur)))))

    om/IRenderState
    (render-state [this state]
      (let [all-cards (:cards app)]
        (dom/div nil
                 (dom/div #js {:className "prompt"} ">")
          (dom/input #js {:type "text" :placeholder "Type your commands here..." :autoFocus "autofocus"
                          :value (:text state)
                          :onChange #(handle-change % owner state)
                          :onKeyDown #(when (== (.-keyCode %) 13)
                                         (dispatch-input dispatcher-chan (:text state))
                                         (om/set-state! owner :text ""))} "")
          (dom/div #js {:className "conversation"}
                   (if (= (count all-cards) 0)
                     (empty-card)
                     (apply dom/ul #js {:className "list"}
                            (om/build-all card-view-om all-cards))))

          (dom/a #js {:className "clear-btn" :href "#" :onClick #(reset! app-state {:cards []})} "Clear"))))))


(defn register-dispatcher [respond-name dispatcher desc]
  (swap! dispatchers assoc respond-name {:exec dispatcher :desc desc}))

(defn register-card [card-name card]
  (swap! cards assoc card-name card))


(defn update-styles []
  (set! (.-innerHTML (. js/document (getElementById "plugin-styles")))
      (css @styles)))

(defn register-css
  "Register plugin css for card"
  [css-content]
  (swap! styles conj css-content)
  (update-styles))


(defn valid-config [names msg & info-type]
  "check if the config exist
  example arguments: :jira :key"
  (let [c (config)]
    (if-not (get-in c names)
      (not (go (>! dispatcher-chan {:type :info-card :content msg :info-type "error" :title "Configuration value[s] missing in ~/.assistant"}))) ;; this case should return true
      true)))


;; built in help dispatcher and card
(defn help-dispatcher [result-chan text]
  (go
    (>! result-chan {:type :help :content (map #(:desc %) (vals @dispatchers)) })))

(defn help-card [app owner]
  (reify
    om/IRender
    (render [this]
      (let [commands (-> app :content )
            commands-text (filter #(not= nil %) commands)
            commands-map (map #(hash-map :name (nth (.split % "--") 0) :desc (nth (.split % "--") 1)) commands-text)]
        (dom/div nil
                 (dom/h2 nil "Available Commands")
                 (apply dom/ul nil (map #(dom/li nil
                                                 (dom/span #js {:className "label"} (:name %))
                                                 (dom/span #js {:className "desc"} (:desc %))) commands-map)))))))

(register-dispatcher :help help-dispatcher "help -- show the list of available commands")
(register-card :help help-card)


(defn clear-dispatcher [result-chan text]
  "Built in dispatcher to clear all cards"
  (reset! app-state {:cards []}))

(register-dispatcher :clear clear-dispatcher "clear -- clear all cards.")

(defn refresh-app-state! []
  (reset! app-state (read-app-state)))

(om/root
  app-view ;; om component
  app-state ;; app state
  {:target (. js/document (getElementById "app"))})



