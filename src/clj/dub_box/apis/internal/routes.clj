(ns dub-box.apis.internal.routes
  (:require [cheshire.core :refer [parse-string]]
            [clojure.string]
            [com.rpl.specter :as sp]
            [dub-box.apis.internal.language :as language]
            [dub-box.apis.internal.transcriptions :as transcriptions]
            [dub-box.apis.internal.video-content :as video-content]
            [dub-box.config :refer [env]]
            [dub-box.middleware.auth :as auth-middleware]
            [dub-box.middleware.exception :as exception]
            [dub-box.middleware.formats :as formats]
            [dub-box.middleware.logger :refer [wrap-logger]]
            [dub-box.utils.elevan-labs :refer [generate-audio]]
            [dub-box.utils.open-ai :refer [make-chat-request]]
            [reitit.coercion.spec :as spec-coercion]
            [reitit.ring.coercion :as coercion]
            [reitit.ring.middleware.multipart :as multipart]
            [reitit.ring.middleware.muuntaja :as muuntaja]
            [reitit.ring.middleware.parameters :as parameters]
            [reitit.swagger :as swagger]
            [reitit.swagger-ui :as swagger-ui]
            [wkok.openai-clojure.api :as api]))

(defn respond-to-msg-handler
  [{:keys [body-params]}]
  (-> (make-chat-request [{:role "system"
                           :content "You name is Jezebel and you are trying to get the inventory levels of the following products: \n1. Chorizo wrap from ML Vegan kitchen, it's a collard green wrap. \nYou are calling a local healthfood store and speaking to a store employee. You are to try to get the price and stock count in a nice and pleasant way from the employee. Your responses should have punctuation for the purpose of being converted to audio, it should sounds natural and not robotic. In order to achieve this, you need to write text for the purpose of being spoken, not read. \n Please be casual and informal. Shorten and conjugate when you can."}
                          {:role "user"
                           :content (:text body-params)}])
      generate-audio)
  {:status 200
   :body
   {:result {:msg "OK"}}})

(defn routes []
  [""
   {:coercion spec-coercion/coercion
    :muuntaja formats/instance
    :swagger {:id ::api}
    :middleware [;; query-params & form-params
                 parameters/parameters-middleware
                 ;; content-negotiation
                 muuntaja/format-negotiate-middleware
                 ;; encoding response body
                 muuntaja/format-response-middleware
                 ;; exception handling
                 exception/exception-middleware
                 ;; decoding request body
                 muuntaja/format-request-middleware
                 ;; coercing request parameters
                 coercion/coerce-request-middleware
                 ;; coercing response bodys
                 coercion/coerce-response-middleware
                 ;; multipart
                 multipart/multipart-middleware
                 ;; logger middleware
                 wrap-logger]}

   ["/apis/internal" {:swagger {:info {:title "Dub Box - API"
                                       :description "https://cljdoc.org/d/metosin/reitit"}}
                      :middleware [reitit.swagger/swagger-feature]}

    ["/swagger.json"
     {:get {:no-doc true
            :handler (swagger/create-swagger-handler)}}]

    ["/api-docs/*"
     {:get {:no-doc true
            :handler (swagger-ui/create-swagger-ui-handler
                      {:url "/api/swagger.json"
                       :config {:validator-url nil}})}}]

    ["/api"
     {:parameters {:header {:x-api-key string?}}
      :middleware [auth-middleware/wrap-api-key]}


     video-content/routes
     language/routes
     transcriptions/routes]]])


(defn parse-and-call
  [{:keys [name arguments]}]
  (let [parsed (parse-string arguments true)]

    {:fn name
     :args parsed}))

(defn nl->normalized-recipe
  [lines]
  (->> (api/create-chat-completion {:model "gpt-3.5-turbo-0613"
                                    :messages [{:role "system" :content "You take recipe information and calculate the total cost"}
                                               {:role "user" :content (clojure.string/join "\n" lines)}]
                                    :functions [{:name "process-recipe-cost",
                                                 :description "Calculate the cost of a recipe based on it's line items",
                                                 :parameters {:type "object",
                                                              :properties
                                                              {:line-items
                                                               {:type "array",
                                                                :items
                                                                {:type "object",
                                                                 :properties
                                                                 {:name {:type "string"},
                                                                  :qty {:type "integer", :minimum 1},
                                                                  :uom {:type "string"}}}}}}}

                                                {:name "suggest-ingredient",
                                                 :description "Suggest the next ingredient to add to the recipe based on the current ingredients",
                                                 :parameters {:type "object",
                                                              :properties
                                                              {:line-items
                                                               {:type "array",
                                                                :items
                                                                {:type "object",
                                                                 :properties
                                                                 {:name {:type "string"},
                                                                  :qty {:type "integer", :minimum 1},
                                                                  :uom {:type "string"}}}}}}}]}
                                   {:api-key (:openai-api-key env)})

       (sp/select-one! [:choices sp/ALL first :message :function_call])
       parse-and-call))


(comment

  (nl->normalized-recipe ["1 1/12 lbs of chicken breast"
                          "10-12 grams of salt"
                          "about a cup of apple cider vinegar"
                          "about 2-3 tsp of garlic powder"])


  (nl->normalized-recipe ["1 1/12 lbs of chicken breast"
                          "10-12 grams of salt"
                          "about a cup of apple cider vinegar"
                          "about 2-3 tsp of garlic powder"])

;;Keep from folding
  )
