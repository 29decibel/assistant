(ns assistant.cards.link-list-card
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:require [assistant.common :as common]
            [assistant.core :refer [register-card register-dispatcher register-css]]
            [cljs-http.client :as http]
            [cljs.core.async :refer [<! >! chan]]
            [hickory.core :as hk]
            [hickory.select :as s]
            [om.core :as om :include-macros true]
            [om.dom :as dom :include-macros true]))



(defn link-list-card [app owner]
  (reify
    om/IRender
    (render [this]
      (apply dom/ul nil (map #(dom/li nil
                                      (common/link (or (-> % :url) (-> % :href))
                                                   (dom/img #js {:src (str "https://www.google.com/s2/favicons?domain=" (-> % :href))})
                                                   (-> % :title))
                                      (dom/p #js {:className "desc"} (-> % :desc))) (:content app))))))


(register-card :link-list-card link-list-card)
(register-css [:.link-list-card
                        [ :li
                         {:margin-bottom "8px"}
                         [:.desc {:color "#999" :margin-top "5px" :margin-left "25px"}]
                         [:img {:display "inline-block" :margin-right "10px"}]]])
