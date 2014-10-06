(ns assistant.services.timezone
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:require [cljs-http.client :as http]
            [assistant.core :refer [register-card register-dispatcher register-css config valid-config]]
            [cljs.core.async :refer [<! >! chan]]
            [hickory.core :as hk]
            [hickory.select :as s]
            [om.core :as om :include-macros true]
            [om.dom :as dom :include-macros true]))

(def api-key (-> (config)
                 :timezone :key))

(def config-err-msg "This command needs a API key from http://api.worldweatheronline.com.
                    You can apply one for free on their website. Then put the key at ~/.assistant as following:
                    {:timezone {:key '...'}}")


(defn time-dispatcher [result-chan text]
  (if (valid-config [:timezone :key] config-err-msg)
    (go (let [url (str "http://api.worldweatheronline.com/free/v1/tz.ashx?q=" text "&format=json&key=" api-key)
              response (<! (http/get url {:with-credentials? false}))
              body (:body response)]
          (when-not (-> body :data :error)
            (>! result-chan {:type :timezone :content body :input text}))))))


(defn timezone-card [data owner]
  (reify
    om/IRender
    (render [this]
      (dom/div nil
               (dom/h4 nil (-> data :content :data :request first :query))
               (dom/span nil (-> data :content :data :time_zone first :localtime))))))

(register-dispatcher :timezone time-dispatcher "timezone [place] -- show timezone of given place")
(register-dispatcher :tz time-dispatcher "tz [place] -- show timezone of given place")
(register-card :timezone timezone-card)
(register-css [:.timezone
                        [:span {:font-size "25px" :color "#8f8f8f"}]])
