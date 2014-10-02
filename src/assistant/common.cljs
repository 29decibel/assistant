(ns assistant.common
  (:require [cljs-http.client :as http]
            [cljs.core.async :refer [<! >! chan]]
            [hickory.core :as hk]
            [hickory.select :as s]
            [om.core :as om :include-macros true]
            [om.dom :as dom :include-macros true]))


(def gui (js/require "nw.gui"))

;; link can be opened in the system browser
(defn link [href & content]
  (apply dom/a #js {:href "#"
              :onClick (fn [e] (do (.preventDefault e)
                                   (.openExternal (.-Shell gui) href)))}
         content))
