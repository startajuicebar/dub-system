(ns dub-box.middleware
  (:require [dub-box.env :refer [defaults]]
            [dub-box.middleware.logger :refer [wrap-logger]]
            [ring.middleware.defaults :refer [site-defaults wrap-defaults]]))

(def cors-headers
  {"Access-Control-Allow-Origin"  "*"
   "Access-Control-Allow-Headers" "*"
   "Access-Control-Allow-Methods" "GET, POST, PUT, DELETE, OPTIONS"})

(defn preflight?
  "Returns true if the request is a preflight request"
  [request]
  (= (request :request-method) :options))

(defn wrap-cors
  "Allow requests from all origins - also check preflight"
  [handler]
  (fn [request]
    (let [is-preflight? (preflight? request)]
      (if is-preflight?
        {:status 200
         :headers cors-headers
         :body "preflight complete"}
        (let [response (handler request)]
          (update-in response [:headers]
                     merge cors-headers))))))

(defn wrap-base [handler]
  (-> ((:middleware defaults) handler)
      wrap-cors
      wrap-logger

      (wrap-defaults
       (-> site-defaults
           (assoc-in [:security :anti-forgery] false)
           (dissoc :session)))))