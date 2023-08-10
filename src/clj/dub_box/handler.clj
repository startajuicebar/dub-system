(ns dub-box.handler
  (:require [dub-box.apis.internal.routes :as internal-api-routes]
            [dub-box.env :refer [defaults]]
            [dub-box.middleware :as middleware]
            [mount.core :as mount]
            [reitit.coercion.spec]
            [reitit.ring :as ring]
            [reitit.ring.middleware.dev]
            [reitit.swagger-ui :as swagger-ui]
            [ring.middleware.content-type :refer [wrap-content-type]]
            [ring.middleware.webjars :refer [wrap-webjars]]))

(mount/defstate init-app
  :start ((or (:init defaults) (fn [])))
  :stop  ((or (:stop defaults) (fn []))))

(mount/defstate app-routes
  :start
  (ring/ring-handler
   (ring/router
    [["/" {:get
           {:handler (constantly {:status 301 :headers {"Location" "/api-docs/index.html"}})}}]
     (internal-api-routes/routes)])



   (ring/routes
    (ring/create-resource-handler
     {:path "/"})
    (wrap-content-type (wrap-webjars (constantly nil)))
    (ring/redirect-trailing-slash-handler))))

(defn app []
  (middleware/wrap-base #'app-routes))


;; =================s======================
;; ------------- FIDDLE
;; =======================================

(defn read-it
  [{:keys [body status headers]}]
  {:status status
   :headers headers
   :body (slurp body)})

(comment

  (-> ((app) {:request-method :get
              :uri "/api/coerce"
              :headers {"x-api-key" "123"}
              :query-params {"hey" "1"}})
      read-it)

  ;; Keep from folding
  )