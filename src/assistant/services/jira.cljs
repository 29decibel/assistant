(ns assistant.services.jira
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:require [assistant.common :as common]
            [assistant.core :refer [register-card register-dispatcher register-css config]]
            [cljs-http.client :as http]
            [cljs.core.async :refer [<! >! chan]]
            [hickory.core :as hk]
            [hickory.select :as s]
            [om.core :as om :include-macros true]
            [om.dom :as dom :include-macros true]))

(def endpoint (-> (config) :jira :endpoint))
(def username (-> (config) :jira :username))
(def password (-> (config) :jira :password))

(defn jira-dispatcher [result-chan text]
  (go (let [response (<! (http/get (str endpoint "/rest/api/2/issue/" text) {:basic-auth {:username username :password password}}))
            m (:body response)
            error (:errorMessages m)
            issue-key (:key m)]
        (print m)
        (when issue-key
          (>! result-chan {:type :jira :content m :input text})))))


(defn jira-view [data owner]
  (reify
    om/IRender
    (render [_]
            (dom/div #js {:className "clearfix"}
                     (common/link (str endpoint "/browse/" (:input data)) (dom/h4 nil (-> data :content :key) "  " (-> data :content :fields :summary)))
                     (dom/p #js {:dangerouslySetInnerHTML #js {:__html (clojure.string/replace (or
                                                                                                 (-> data :content :fields :description) "") #"\n" "<br>")}} nil)))))


(register-dispatcher :jira jira-dispatcher "jira [issue-number] -- find jira issue")
(register-card :jira jira-view)
