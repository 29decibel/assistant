(ns assistant.services.map
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:require [cljs-http.client :as http]
            [assistant.core :refer [register-card register-dispatcher register-css config]]
            [cljs.core.async :refer [<! >! chan]]
            [hickory.core :as hk]
            [hickory.select :as s]
            [om.core :as om :include-macros true]
            [om.dom :as dom :include-macros true]))


(def api-key (-> (config)
                 :map :key))


(defn map-dispatcher [result-chan text]
  (go
    (>! result-chan {:type :map :content (str "https://www.google.com/maps/embed/v1/place?key=" api-key "&q=" text) :input text})))

(defn map-view [data owner]
  (reify
    om/IRender
    (render [_]
            (dom/iframe #js {:src (:content data)} nil))))


(register-dispatcher :map map-dispatcher "map [place] -- show the map of given place name")
(register-card :map map-view)
(register-css [:.map
               [:iframe {:width "100%" :height "400px" :border "none"}]
               [:img {:margin-top "2px auto"}]])
