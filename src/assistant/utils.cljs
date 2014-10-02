(ns assistant.utils
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:require [cljs-http.client :as http]
            [cljs.core.async :refer [<! >! chan]]
            [hickory.core :as hk]
            [hickory.render :as hk-render]
            [hickory.select :as s]
            [om.core :as om :include-macros true]
            [om.dom :as dom :include-macros true]))

(def fs (js/require "fs"))

(defn- get-text [tags]
  (apply str (filter #(> (count (clojure.string/trim %)) 0)
          (flatten (map (fn [tag] (let [t (goog/typeOf tag)
                                        cv (:content tag)]
                                    (if (= t "string")
                                      tag
                                      (if cv (get-text cv) "")))) tags)))))

(defn user-home []
  (let [node-env (.-env js/process)]
    (or (.-HOME node-env) (.-HOMEPATH node-env) (.-USERPROFILE node-env))))

(defn create-if-not-exist
  "create file if the file not exists"
  [file-name]
  (if-not (.existsSync fs file-name)
    (.writeFileSync fs file-name "" "utf-8")))


(defn write-to-file [file-name content]
  (.writeFileSync fs file-name content "utf-8"))



