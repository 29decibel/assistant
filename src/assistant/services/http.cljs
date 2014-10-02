(ns assistant.services.http
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:require [assistant.common :as common]
            [assistant.core :refer [register-card register-dispatcher register-css]]
            [cljs.reader :as reader]
            [cljs-http.client :as http]
            [cljs.core.async :refer [<! >! chan]]
            [hickory.core :as hk]
            [hickory.select :as s]
            [om.core :as om :include-macros true]
            [om.dom :as dom :include-macros true]))

(def urls {:header "https://raw.githubusercontent.com/for-GET/know-your-http-well/master/json/headers.json"
           :status "https://raw.githubusercontent.com/for-GET/know-your-http-well/master/json/status-codes.json"
           :method "https://raw.githubusercontent.com/for-GET/know-your-http-well/master/json/methods.json"})

(def title-field {:header "header"
                  :status "code"
                  :method "method"})

(def gui (js/require "nw.gui"))

(defn http-dispatcher [result-chan text]
  (go (let [url (get urls (keyword text))
            response (<! (http/get url {:with-credentials? false}))
            m (js->clj (.parse js/JSON (-> response :body )))]
        (when m
          (>! result-chan {:type :http :content m :input text})))))

(defn http-card [data owner]
  (reify
    om/IRender
    (render [this]
      (let [shots (:content data)
            input (:input data)]
        (apply dom/ul #js {:className "clearfix"} (map #(dom/li nil
                                                                (dom/span nil (get % (get title-field (keyword input))))
                                                                (dom/p nil (get % "description"))) shots))))))

;; export

(register-dispatcher :http http-dispatcher "http -- know your http well")
(register-card :http http-card)
(register-css [:.http
                        [ :ul
                         {:height "500px" :overflow "auto"}
                         [:li
                          {:margin "5px" :position "relative" :padding "10px"}
                          [:h4
                           {:overflow "hidden"}]]]])
