(ns assistant.services.nytimes
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:require [assistant.common :as common]
            [assistant.core :refer [register-card register-dispatcher register-css config]]
            [cljs-http.client :as http]
            [cljs.core.async :refer [<! >! chan]]
            [hickory.core :as hk]
            [hickory.select :as s]
            [om.core :as om :include-macros true]
            [om.dom :as dom :include-macros true]))

(def url "http://api.nytimes.com/svc/books/v2/lists/")

(def api-key (-> (config)
                 :nytimes :key))

(defn nytimes-dispatcher [result-chan text]
  (go (let [response (<! (http/get (str url text ".json?api-key=" api-key) {:with-credentials? false}))
            m (-> response :body :results)
            result (map #(assoc %
                                :title (-> % :book_details first :title)
                                :image (-> % :book_details first :book_image)
                                :url (-> % :book_details first :amazon_product_url)) m)]
        (when (and m (> (count m) 0))
          (>! result-chan {:type :list-image-card :content result :input text})))))


(register-dispatcher :nytimes nytimes-dispatcher "nytimes [fiction/nonfiction] -- show newyork times best seller books")
