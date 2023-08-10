(ns dub-box.utils.mux
  (:require [cheshire.core :refer [generate-string parse-string]]
            [clj-http.client :as client]
            [dub-box.config :refer [env]])

  (:gen-class))

(def assets-endpoint "https://api.mux.com/video/v1/assets")

(defn basic-auth
  []
  [(-> env :mux :access-token) (-> env :mux :secret-key)])

(defn create-asset!
  [url]
  (let [result (client/post assets-endpoint
                            {:content-type "application/json"
                             :basic-auth (basic-auth)
                             :body (generate-string {:input [{:url url}]
                                                     :playback_policy ["public"]})})
        parsed-result (-> (:body result)
                          (parse-string keyword))]

    parsed-result))