(ns assistant.services.hackernews
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:require [assistant.common :as common]
            [assistant.core :refer [register-card register-dispatcher register-css]]
            [cljs-http.client :as http]
            [cljs.core.async :refer [<! >! chan]]
            [hickory.core :as hk]
            [hickory.select :as s]
            [om.core :as om :include-macros true]
            [om.dom :as dom :include-macros true]))


(defn- get-links [body]
  (let [hk-tree (-> body hk/parse hk/as-hickory)
        links (-> (s/select (s/child
                              (s/and (s/tag :td) (s/class :title))
                              (s/tag :a))
                            hk-tree))
        valid-links (filter #(= (.indexOf (-> % :attrs :href) "http") 0) links)]
    valid-links))


(defn hn-dispatcher [result-chan text]
  (go (let [response (<! (http/get "https://news.ycombinator.com/" {:with-credentials? false}))
            body (:body response)
            links  (get-links body)
            links2 (map #(assoc % :title (-> % :content first) :href (-> % :attrs :href)) links)]
        (>! result-chan {:type :link-list-card :content links2 :input text}))))


(register-dispatcher :hn hn-dispatcher "hn -- show hacker news ")
