(ns dub-box.utils.open-ai
  (:require [dub-box.config :refer [env]]
            [cheshire.core :refer [generate-string parse-string]]
            [dub-box.utils.stash :refer [stash peek-stash]]
            [com.rpl.specter :as sp]
            [clojure.edn :as edn]
            [dub-box.utils.debug :refer [spy]]
            [dub-box.utils.lorem :as lorem-utils]
            [clj-http.client :as client])
  (:import [com.knuddels.jtokkit Encodings]
           [com.knuddels.jtokkit.api ModelType]))

(defn token-count
  [text]
  (let [registry (Encodings/newDefaultEncodingRegistry)
        encoding (.getEncodingForModel registry ModelType/GPT_4)]
    (.countTokens encoding text)))

(defn calculate-cost
  [text]
  (let [token-count (token-count text)
        token-1ks (Math/ceil (/ token-count 1000))]
    {:token-count token-count
     :cost (Math/round (* token-1ks 0.06))}))

(defn parse-args
  [x]
  {:func (:name x)
   :args (parse-string (:arguments x) keyword)})

(defn process-function-call
  [res]
  (->> res
       (sp/select-one [:body :choices 0 :message :function_call])
       parse-args))

(defn make-insert-request
  [prompt suffix & {:keys [model temperature max-tokens top-p frequency-penalty presence-penalty]
                    :or {model "text-davinci-003"
                         temperature 0.7
                         max-tokens 256
                         top-p 1
                         frequency-penalty 0
                         presence-penalty 0}}]
  (let [request-payload {:model model
                         :prompt prompt
                         :suffix suffix
                         :temperature temperature
                         :max_tokens max-tokens
                         :top_p top-p
                         :frequency_penalty frequency-penalty
                         :presence_penalty presence-penalty}
        result (client/post "https://api.openai.com/v1/completions"
                            {:content-type "application/json"
                             :headers {:authorization (str "Bearer " (-> env :openai :api-key))}
                             :body (generate-string request-payload)})]
    (-> (:body result)
        (parse-string keyword)
        :choices
        first
        :text
        edn/read-string)))

(defn make-chat-request
  [messages & {:keys [model temperature max-tokens top-p frequency-penalty presence-penalty]
               :or {model "gpt-4"
                    temperature 0.7
                    max-tokens 5000
                    top-p 1
                    frequency-penalty 0
                    presence-penalty 0}}]
  (let [request-payload {:model model
                         :messages messages}
        result (client/post "https://api.openai.com/v1/chat/completions"
                            {:content-type "application/json"
                             :as :json
                             :headers {:authorization (str "Bearer " (-> env :openai :api-key))}
                             :body (generate-string request-payload)})]

    (sp/select-one [:body :choices 0 :message :content] result)))

(meta (with-meta {:text "some text man"} {:url "http://google.com"
                                          :time 100}))

(defn make-function-request
  [messages functions & {:keys [model]
                         :or {model "gpt-4-0613"}}]
  (let [request-payload {:model model
                         :messages messages
                         :functions functions}
        result (client/post "https://api.openai.com/v1/chat/completions"
                            {:content-type "application/json"
                             :as :json
                             :headers {:authorization (str "Bearer " (-> env :openai :api-key))}
                             :body (generate-string request-payload)})]
    result))