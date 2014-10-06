(ns assistant.app
  (:require [assistant.core :refer [refresh-app-state! put-result]]
            [assistant.utils :as utils]))

(def fs (js/require "fs"))

(def plugins-file (str (utils/user-home) "/.assistant-plugins"))
(utils/create-if-not-exist plugins-file)


(defn start-with-hash [plugin-name]
  (= (.indexOf plugin-name "#") 0))

(defn empty-string [plugin-name]
  (= (.-length plugin-name) 0))

(defn load-plugins []
  (let [file (.readFileSync fs plugins-file "utf-8")
        plugins (->> (.split file "\n")
                     (js->clj)
                     (filter (complement start-with-hash))
                     (filter (complement empty-string)))]
    (doseq [p plugins]
      (.require js/goog p))))


;; register global uncaught exceptions
(.on js/process "uncaughtException"
     (fn [e]
       (put-result {:type :info-card :title "Uncaught Exception:" :content (.-message e) :info-type "error"})))

(print "Loading third part plugins from ~/.assistant-plugins...")
(load-plugins)

(print "Restore app state .")
(refresh-app-state!)
