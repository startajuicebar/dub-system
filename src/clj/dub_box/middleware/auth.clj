(ns dub-box.middleware.auth
  (:require [cheshire.core :refer [generate-string]]
            [clojure.set]
            [clojure.string :as string :refer [split]]
            [dub-box.config :refer [env]]))

(defn has-permissions?
  [required allowed]
  (let [allowed (set allowed)
        required (set required)]
    (clojure.set/subset? allowed required)))

(defn wrap-role-authorized
  [handler required]
  (fn [request]
    (let [{:keys [permissions]} request]
      (if (has-permissions? permissions required)
        (handler request)
        {:status 401
         :body   (str "You are not authorized "
                      (generate-string {:required required
                                        :permissions permissions}))}))))

(defn wrap-api-key
  [handler]
  (fn [request]
    (let [x-api-key (-> request :parameters :header :x-api-key)]
      (if (= x-api-key (env :api-key))
        (handler request)
        {:status 401
         :body "You are not authorized with that api-key "}))))


(defn extract-jwt-from-bearer-header
  [bearer-token]
  (-> bearer-token
      (split #"Bearer ")
      last))

(defn wrap-jwt*
  "
   `wrap-jwt` is a middleware function for handling JSON Web Token (JWT) authentication in a Clojure web application that uses Auth0 for authentication. It validates the JWT, extracts the necessary data, and attaches it to the request object, before passing it to the next handler in the chain.
   
   Usage:
   (defn wrap-jwt [handler])
   
   Parameters:
   - `handler`: A function that represents the next handler in the chain.

   The function expects an incoming request with an \"Authorization\" header containing a valid JWT, following the \"Bearer <token>\" format.

   The `wrap-jwt` middleware performs the following steps:

   1. Extracts the JWT from the \"Authorization\" header.
   2. Decodes and validates the JWT using `jwt-utils/decode-jwt`. The decoded JWT data is stored in `decoded-jwt`.
   3. If the JWT is valid, it retrieves the client ID associated with the Auth0 user ID (using `client-user-model/get-client-id-by-auth0-user-id`), and adds it to the decoded JWT data.
   4. The `auth-data` map, containing the decoded JWT and client ID, is associated with the incoming request under the key `:auth-data`.
   5. The modified request is passed to the next handler in the chain.

   If the JWT is invalid or not present, the function returns a 401 Unauthorized response with the message \"You are not authorized with that Bearer token\".

   Example:

   (def app
     (-> (routes my-routes)
         (wrap-jwt)))

   In this example, the `wrap-jwt` middleware is applied to the `my-routes` handler. When processing a request, the middleware will ensure that a valid JWT is present in the \"Authorization\" header, and will add the associated client ID to the request's `:auth-data` key.
   "
  [{:keys [decoder-fn
           matcher-fn]} handler]
  (fn [request]
    (let [bearer-token (-> request :parameters :header :authorization)
          encoded-token (extract-jwt-from-bearer-header bearer-token)
          decoded-jwt (decoder-fn encoded-token)
          is-valid? (:is-valid? decoded-jwt)]
      (if is-valid?
        (let [auth-data (matcher-fn decoded-jwt)]
          (handler (assoc request :auth-data auth-data)))
        {:status 401
         :body "You are not authorized with that Bearer token "}))))

(defn wrap-jwt
  [opts]
  (partial wrap-jwt* opts))