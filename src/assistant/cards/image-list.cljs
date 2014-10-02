(ns assistant.cards.list-card
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:require [assistant.common :as common]
            [assistant.core :refer [register-card register-dispatcher register-css config]]
            [cljs-http.client :as http]
            [cljs.core.async :refer [<! >! chan]]
            [hickory.core :as hk]
            [hickory.select :as s]
            [om.core :as om :include-macros true]
            [om.dom :as dom :include-macros true]))

;; list-image-card
(defn list-image-card [data owner]
  "Om component -- list image card, expecting an array of maps.
  Each value in the array is {:title \"some title\" :image \"image url\" :subtitle \"subtitle\" :url}"
  (reify
    om/IRender
    (render [this]
      (let [items (:content data)]
        (apply dom/ul #js {:className "clearfix"} (map #(dom/li nil
                                                                (common/link (:url %)
                                                                                 (dom/h4 #js {:title (:title %)} (:title %))
                                                                               (dom/img #js {:src (:image %)} nil))
                                                              (dom/span #js {:className "subtitle" } (:subtitle %))) items))))))

(register-card :list-image-card list-image-card)
(register-css [:.list-image-card
                        [:.subtitle {
                                 :position "absolute"
                                 :left "2px"
                                 :bottom "3px"
                                 :opacity ".8"
                                 :color "white"
                                 :font-size "30px" }]
                        [ :ul
                         [:li
                          {:float "left" :margin "5px" :position "relative"}
                          [:&:hover [:.subtitle {:color "#ccc"}]]
                          [:img {:height "200px"}]
                          [:h4
                           {:max-width "130px" :text-overflow "ellipsis" :white-space "nowrap" :overflow "hidden"}]]]])
