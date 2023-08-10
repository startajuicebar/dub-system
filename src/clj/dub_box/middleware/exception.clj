(ns dub-box.middleware.exception
  (:require [clojure.tools.logging :as log]
            [expound.alpha :as expound]
            [reitit.ring.middleware.exception :as exception]))

(defn coercion-error-handler [status]
  (let [printer (expound/custom-printer {:print-specs? false})]
    (fn [exception _]
      (let [problems (-> exception ex-data :problems)
            message (if (= status 400)
                      "Couldn't coerce the reqest"
                      "Couldn't coerce the response")]
        (log/error (with-out-str (printer problems)))

        {:status status
         :body {:message message}}))))

(defn custom-error
  [status]
  (fn [exception _]
    (let [{:keys [cause explain]} (-> exception ex-data)]
      {:status status
       :body {:cause cause
              :explain explain}})))


(def exception-middleware
  (exception/create-exception-middleware
   (merge
    exception/default-handlers
    {;; Handle request coercion errors
     :reitit.coercion/request-coercion (coercion-error-handler 400)
     ;; Handle response coercion errors
     :reitit.coercion/response-coercion (coercion-error-handler 500)
     ;; Catch schema exceptions
     :models/schema-error (custom-error 422)
     ;; Log stack-traces for all exceptions
     ::exception/wrap (fn [handler e request]
                        (log/error e (.getMessage e))
                        (handler e request))})))