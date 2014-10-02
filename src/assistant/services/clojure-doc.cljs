(ns assistant.services.clojure
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:require [assistant.common :as common]
            [assistant.core :refer [register-card register-dispatcher register-css]]
            [cljs-http.client :as http]
            [cljs.core.async :refer [<! >! chan]]
            [hickory.core :as hk]
            [hickory.select :as s]
            [om.core :as om :include-macros true]
            [om.dom :as dom :include-macros true]))

(def url "http://api.clojuredocs.org/examples/clojure.core/")

(defn clojure-doc-dispatcher [result-chan text]
  (go (let [response (<! (http/get (str url text) {:with-credentials? false}))
            m (-> response :body :examples)]
        (when m
          (>! result-chan {:type :clojure :content m :input text})))))

(defn clojure-doc-card [data owner]
  (reify
    om/IRender
    (render [this]
      (let [docs (:content data)]
        (apply dom/ul #js {:className "clearfix"} (map #(dom/li nil (dom/pre nil (dom/code nil (:body %)))) docs))))))

(register-dispatcher :clojure clojure-doc-dispatcher "clojure [function name] -- Your awesome clojure doc loop up")
(register-dispatcher :clj clojure-doc-dispatcher "clj [function name] -- Your awesome clojure doc loop up")
(register-card :clojure clojure-doc-card)
(register-css [:.clojure
                        [ :ul
                         [:li
                          [:&:last-child {:margin-bottom "5px"}]
                          [:&:last-child:after {:content "\"\""}]
                          [:&:after {:content (str "\"" (apply str (repeat 100 "*")) "\"")}]
                          {:float "left"
                           :margin "5px"
                           :margin-bottom "20px"
                           :width "100%"
                           :overflow "auto"
                           :position "relative"}
                          [:h4
                           {:max-width "130px" :text-overflow "ellipsis" :white-space "nowrap" :overflow "hidden"}]]]])
