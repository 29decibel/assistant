(ns assistant.cards.info-card
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:require [assistant.common :as common]
            [assistant.core :refer [register-card register-dispatcher register-css]]
            [cljs-http.client :as http]
            [cljs.core.async :refer [<! >! chan]]
            [hickory.core :as hk]
            [hickory.select :as s]
            [om.core :as om :include-macros true]
            [om.dom :as dom :include-macros true]))


(defn info-card [{:keys [content info-type]} owner]
  "info-card is a generate card to display info, warn or error messages to user"
  (reify
    om/IRender
    (render [this]
      (dom/div #js {:className info-type } (dom/h2 nil (.toUpperCase info-type))
               content))))

(register-card :info-card info-card)
(register-css [:.info-card
                        [:.error {:color "#DA4453"}]
                        [:.success {:color "#8CC152"}]
                        [:.info {:color "#4A89DC"}]
                        [:.warn {:color "#F6BB42"}]])

