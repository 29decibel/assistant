(ns assistant.services.github
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:require [assistant.common :as common]
            [assistant.utils :as ass-utils]
            [assistant.core :refer [register-card register-dispatcher register-css]]
            [cljs-http.client :as http]
            [cljs.core.async :refer [<! >! chan]]
            [hickory.core :as hk]
            [hickory.render :as hk-render]
            [hickory.select :as s]
            [om.core :as om :include-macros true]
            [om.dom :as dom :include-macros true]))


(defn printcol [col]
  (doall (map print col)))


(defn- get-projects [body]
  (let [hk-tree (-> body hk/parse hk/as-hickory)
        projects (-> (s/select (s/child
                              (s/and (s/tag :li) (s/class :repo-list-item)))
                            hk-tree))]
    projects))


(defn- p-link [content]
  (let [link (-> (s/select (s/child (s/class :repo-list-name) (s/tag :a)) content) first)]
    link))

(defn- p-desc [content]
  (let [desc (-> (s/select (s/child (s/class :repo-list-description)) content) first)]
    desc))

(defn- p-meta [content]
  (let [desc (-> (s/select (s/child (s/class :repo-list-meta)) content) first)]
    desc))

(defn github-dispatcher [result-chan text]
  (go (let [url (str "https://github.com/trending?l=" text)
            response (<! (http/get url {:with-credentials? false}))
            body (:body response)
            projects (get-projects body)
            infos (map #(hash-map :url (str "http://github.com" (-> % p-link :attrs :href))
                                  :title (-> % p-link :content ass-utils/get-text)
                                  :desc (-> % p-desc :content ass-utils/get-text)
                                  :desc2 (-> % p-meta :content ass-utils/get-text)
                                  ) projects)]
          (>! result-chan {:type :link-list-card :content infos :input text}))))

(defn- remove-redundent-text
  [text]
  (.replace text "• Built by" ""))

(register-dispatcher :gh github-dispatcher "gh [language] -- show trending repositories in Github of [language] or all")
