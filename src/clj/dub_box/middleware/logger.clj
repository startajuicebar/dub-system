(ns dub-box.middleware.logger
  (:require [cheshire.core :refer [generate-string]]
            [clojure.walk :as walk]
            [taoensso.timbre :as timbre :refer [info]]))

(defn replace-keys
  "Redacts the values found in m for each key in redact-keys.
  The redacted value is obtained by applying redact-value-fn to key and value"
  [m {:keys [replace-key?]}]
  (walk/postwalk (fn [x]
                   (if (map? x)
                     (->> x
                          (map (fn [[k v]]
                                 (if (replace-key? (keyword k))
                                   [k ((constantly "[REDACTED]") k v)]
                                   [k v])))
                          (into {}))
                     x))
                 m))

(defn redact-private-header-data
  "Redacts private header information"
  [request]
  (let [redacted-headers (replace-keys (:headers request) {:replace-key? #{:x-api-key :authorization :password}})]
    (assoc request :headers redacted-headers)))

(defn wrap-logger
  "Logger middleware"
  [handler]
  (fn [request]
    (-> (redact-private-header-data request)
        (select-keys [:headers :body-params :params])
        generate-string
        info)
    (handler request)))