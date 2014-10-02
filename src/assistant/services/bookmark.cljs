(ns assistant.services.bookmark
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:require [assistant.common :as common]
            [assistant.core :refer [register-card register-dispatcher register-css]]
            [cljs-http.client :as http]
            [cljs.core.async :refer [<! >! chan]]
            [hickory.core :as hk]
            [hickory.select :as s]
            [om.core :as om :include-macros true]
            [om.dom :as dom :include-macros true]))

(def database (atom []))

(defn- get-title [body]
  (let [hk-tree (-> body hk/parse hk/as-hickory)
        items (-> (s/select (s/child (s/tag :title)) hk-tree))]
    (-> items first :content first)))

(defn bookmark-dispatcher [result-chan text]
  (go (let [response (<! (http/get text {:with-credentials? false}))
            m (-> response :body)
            title (get-title m)]
        (swap! database conj {:title title :url text})
        (when m
          (>! result-chan {:type :bookmark :content @database :input text})))))

(defn bookmark-card [data owner]
  (reify
    om/IRender
    (render [this]
      (let [links (:content data)]
        (apply dom/ul #js {:className "clearfix"} (map #(dom/li nil
                                                                (common/link (:url %)
                                                                             (dom/img #js {:src (str "https://www.google.com/s2/favicons?domain=" (:url %))})
                                                                             (dom/h4 nil (:title %)))) links))))))

(register-dispatcher :bookmark bookmark-dispatcher "bookmark -- add bookmark")
(register-dispatcher :bm bookmark-dispatcher "bm -- add bookmark")
(register-card :bookmark bookmark-card)
(register-css [:.bookmark
                        [ :ul
                         [:li
                          [:img {:float "left" :margin-right "10px"}]]]])
