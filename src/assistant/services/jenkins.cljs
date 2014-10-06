(ns assistant.services.jenkins
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:require [assistant.common :as common]
            [assistant.core :refer [register-card register-dispatcher register-css config valid-config]]
            [cljs-http.client :as http]
            [cljs.core.async :refer [<! >! chan]]
            [hickory.core :as hk]
            [hickory.select :as s]
            [om.core :as om :include-macros true]
            [om.dom :as dom :include-macros true]))

;; get all jobs
;; curl -u username:password HOST/api/json

;; outputs from build
;;  curl -X POST -u username:password HOST/job/JOB_NAME/lastBuild/consoleText

;; trigger build
;; curl -X POST -u username:password HOST/job/JOB_NAME/build\?token\=TOKEN

(def host (-> (config) :jenkins :host))
(def token (-> (config) :jenkins :token))
(def username (-> (config) :jenkins :username))
(def password (-> (config) :jenkins :password))
(def basic-auth {:basic-auth {:username username :password password}})

(def config-err-msg "Please make sure following config exists in your ~/.assistant: :jenkins {:host :username :password :token}")

(defn check-jenkins-config []
  (and (valid-config [:jenkins :host] config-err-msg)
       (valid-config [:jenkins :token] config-err-msg)
       (valid-config [:jenkins :username] config-err-msg)
       (valid-config [:jenkins :password] config-err-msg)))

(defn jenkins-dispatcher [result-chan text]
  (if (check-jenkins-config)
    (go (let [response (<! (http/get (str host "/api/json") basic-auth))
              m (:body response)
              result (map #(assoc % :title (:name %)) (:jobs m))]
          (>! result-chan {:type :jenkins-card :content result :input text})))))

(def console-chan (chan))

(defn build-job [job-name result-chan]
  "Trigger job and then collect build log to result-chan"
  (go (let [response (<! (http/post (str host "/job/" job-name "/build?token=" token) basic-auth))
            status (:status response)]
        (print status)
        (>! result-chan (str job-name " build started...")))))

  ;(js/setInterval (fn []
                    ;(go (let [response (<! (http/post (str host "/job/" job-name "/lastBuild/consoleText")))
                              ;m (:body response)]
                          ;(>! result-chan m))) 6000)))

(defn jenkins-card [app owner]
  (reify
    om/IInitState
    (init-state [_]
      {:console-text ""})

    om/IWillMount
    (will-mount [_]
      (go (loop []
            (let [result (<! console-chan)]
              (om/set-state! owner :console-text result)
              (recur)))))

    om/IRenderState
    (render-state [this state]
      (dom/div nil
        (apply dom/ul nil (map (fn [job] (dom/li nil
                                        (common/link (or (-> job :url) (-> job :href)) (-> job :title))
                                        (dom/button #js {:onClick #(build-job (:name job) console-chan)} "Build"))) (:content app)))
        (dom/h4 nil "Built console logs: ")
        (dom/code #js {:className "console-text"} (:console-text state)) ))))


(register-dispatcher :ci jenkins-dispatcher "ci -- show jenkins panel")
(register-card :jenkins-card jenkins-card)
(register-css [:.jenkins-card
               [:code {:max-height "30px" :height "30px" :overflow "auto"}]
               [:a {:display "inline-block" :min-width "200px"}]])
