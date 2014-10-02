(ns assistant.cards.markdown-card
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:require [assistant.common :as common]
            [assistant.core :refer [register-card register-dispatcher register-css]]
            [cljs-http.client :as http]
            [markdown.core :refer [md->html]]
            [cljs.core.async :refer [<! >! chan]]
            [hickory.core :as hk]
            [hickory.select :as s]
            [om.core :as om :include-macros true]
            [om.dom :as dom :include-macros true]))

(defn md-dispatcher [result-chan text]
  (go
    (>! result-chan {:type :markdown-card :content "##this is title \n### nice \n ```console.log('nice');```" :input text})))


(defn markdown-card [app owner]
  (reify
    om/IRender
    (render [this]
      (dom/div #js {:dangerouslySetInnerHTML #js {:__html (md->html (:content app)) }} nil))))

(register-dispatcher :md md-dispatcher "md [issue-number] -- find jira issue")
(register-card :markdown-card markdown-card)
