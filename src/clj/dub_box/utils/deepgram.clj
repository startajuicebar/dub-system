(ns dub-box.utils.deepgram
  (:require
   [clj-http.client :as client]
   [clojure.string :as str]
   [cheshire.core :as json]
   [cheshire.core :refer [generate-string parse-string]]
   [dub-box.utils.audio :refer [play-audio]]
   [com.rpl.specter :as sp]
   [clojure.java.io :as io]
   [dub-box.config :refer [env]])

  (:import
   [java.io FileInputStream]
   [java.net URL]
   [java.io ByteArrayOutputStream ByteArrayInputStream FileOutputStream])
  (:gen-class))

(def languages
  [{:code "zh"
    :language "Chinese"
    :models-tiers [{:model "general" :tiers ["base"]}]}
   {:code "zh-CN"
    :language "Chinese, China"
    :models-tiers [{:model "general" :tiers ["base"]}]}
   {:code "zh-TW"
    :language "Chinese, Taiwan"
    :models-tiers [{:model "general" :tiers ["base"]}]}
   {:code "da"
    :language "Danish"
    :models-tiers [{:model "general" :tiers ["enhanced" "base"]}]}
   {:code "nl"
    :language "Dutch"
    :models-tiers [{:model "general" :tiers ["enhanced" "base"]}]}
   {:code "en"
    :language "English"
    :models-tiers [{:model "general" :tiers ["nova" "enhanced" "base"]}
                   {:model "meeting" :tiers ["enhanced" "base"]}
                   {:model "phonecall" :tiers ["nova" "enhanced" "base"]}
                   {:model "voicemail" :tiers ["base"]}
                   {:model "finance" :tiers ["enhanced" "base"]}
                   {:model "conversationalai" :tiers ["base"]}
                   {:model "video" :tiers ["base"]}]}
   {:code "en-AU"
    :language "English, Australia"
    :models-tiers [{:model "general" :tiers ["nova" "base"]}]}
   {:code "en-GB"
    :language "English, United Kingdom"
    :models-tiers [{:model "general" :tiers ["nova" "base"]}]}
   {:code "en-IN"
    :language "English, India"
    :models-tiers [{:model "general" :tiers ["nova" "base"]}]}
   {:code "en-NZ"
    :language "English, New Zealand"
    :models-tiers [{:model "general" :tiers ["nova" "base"]}]}
   {:code "en-US"
    :language "English, United States"
    :models-tiers [{:model "general" :tiers ["nova" "enhanced" "base"]}
                   {:model "meeting" :tiers ["enhanced" "nova" "base"]}
                   {:model "phonecall" :tiers ["enhanced" "base"]}
                   {:model "voicemail" :tiers ["base"]}
                   {:model "finance" :tiers ["enhanced" "base"]}
                   {:model "conversationalai" :tiers ["base"]}
                   {:model "video" :tiers ["base"]}]}
   {:code "nl"
    :language "Flemish"
    :models-tiers [{:model "general" :tiers ["enhanced" "base"]}]}
   {:code "fr"
    :language "French"
    :models-tiers [{:model "general" :tiers ["enhanced" "base"]}]}
   {:code "fr-CA"
    :language "French, Canada"
    :models-tiers [{:model "general" :tiers ["base"]}]}
   {:code "de"
    :language "German"
    :models-tiers [{:model "general" :tiers ["enhanced" "base"]}]}
   {:code "hi"
    :language "Hindi"
    :models-tiers [{:model "general" :tiers ["enhanced" "base"]}]}
   {:code "hi-Latn"
    :language "Hindi, Roman Script"
    :models-tiers [{:model "general" :tiers ["base"]}]}
   {:code "id"
    :language "Indonesian"
    :models-tiers [{:model "general" :tiers ["base"]}]}
   {:code "it"
    :language "Italian"
    :models-tiers [{:model "general" :tiers ["enhanced" "base"]}]}
   {:code "ja"
    :language "Japanese"
    :models-tiers [{:model "general" :tiers ["enhanced" "base"]}]}
   {:code "ko"
    :language "Korean"
    :models-tiers [{:model "general" :tiers ["enhanced" "base"]}]}
   {:code "no"
    :language "Norwegian"
    :models-tiers [{:model "general" :tiers ["enhanced" "base"]}]}
   {:code "pl"
    :language "Polish"
    :models-tiers [{:model "general" :tiers ["enhanced" "base"]}]}
   {:code "pt"
    :language "Portuguese"
    :models-tiers [{:model "general" :tiers ["enhanced" "base"]}]}
   {:code "pt-BR"
    :language "Portuguese, Brazil"
    :models-tiers [{:model "general" :tiers ["enhanced" "base"]}]}
   {:code "pt-PT"
    :language "Portuguese, Portugal"
    :models-tiers [{:model "general" :tiers ["enhanced" "base"]}]}
   {:code "ru"
    :language "Russian"
    :models-tiers [{:model "general" :tiers ["base"]}]}
   {:code "es"
    :language "Spanish"
    :models-tiers [{:model "general" :tiers ["enhanced" "base" "nova"]}]}
   {:code "es-419"
    :language "Spanish, Latin America"
    :models-tiers [{:model "general" :tiers ["enhanced" "base" "nova"]}]}
   {:code "sv"
    :language "Swedish"
    :models-tiers [{:model "general" :tiers ["enhanced" "base"]}]}
   {:code "ta"
    :language "Tamil"
    :models-tiers [{:model "general" :tiers ["enhanced"]}]}
   {:code "tr"
    :language "Turkish"
    :models-tiers [{:model "general" :tiers ["base"]}]}
   {:code "uk"
    :language "Ukrainian"
    :models-tiers [{:model "general" :tiers ["base"]}]}])

(def transcription-endpoint "https://api.deepgram.com/v1/listen")

(defn audio-url->text
  [url & {:keys [model tier version language punctuate smart-format utterances]
          :or {model "general"
               tier "nova"
               version "latest"
               language "en"
               punctuate true
               smart-format true
               utterances true}}]
  (let [query-params {:model model
                      :tier tier
                      :version version
                      :language language
                      :punctuate punctuate
                      :smart_format smart-format
                      :utterances utterances}
        result (client/post transcription-endpoint
                            {:content-type "application/json"
                             :query-params query-params
                             :headers {:authorization (str "Token " (-> env :deep-gram :api-key))}
                             :body (generate-string {:url url})})
        parsed-result (-> (:body result)
                          (parse-string keyword))]

    (sp/select-one [:results :utterances] parsed-result)))

(defn audio->text
  [audio-file & {:keys [model tier version language punctuate smart-format utterances]
                 :or {model "general"
                      tier "nova"
                      version "latest"
                      language "en"
                      punctuate true
                      smart-format true
                      utterances true}}]
  (let [query-params {:model model
                      :tier tier
                      :version version
                      :language language
                      :punctuate punctuate
                      :smart_format smart-format
                      :utterances utterances}
        result (client/post transcription-endpoint
                            {:content-type "audio/mpeg"
                             :query-params query-params
                             :headers {:authorization (str "Token " (-> env :deep-gram :api-key))}
                             :body (io/file audio-file)})
        parsed-result (-> (:body result)
                          (parse-string keyword))]

    (sp/select-one [:results :utterances] parsed-result)))

(comment
  #_(io/file "/Users/atd/Documents/projects/sajb/dubbing-system/sample.mp3")

  (audio->text "/Users/atd/Documents/projects/sajb/dubbing-system/media/samples/sample.mp3")

  #_(audio->text "/Users/atd/Documents/projects/sajb/dubbing-system/sample-audio.flac")


  #_(generate-audio "Persistence during challenging times is vital for achieving long-term goals. Hardships can be reframed as opportunities for growth, offering unparalleled insights into our capabilities and fostering resilience. Sticking with something tough is not just about overcoming obstacles, but also about personal evolution - it teaches us patience, discipline, and adaptability. Each difficulty conquered bolsters confidence, enhancing our problem-solving skills and decision-making ability. This commitment to stick it out, despite adversity, forms the bedrock of success. Remember, breakthroughs often occur when things seem most dire. Hence, embracing difficulty is essential, turning trials into triumphs and unlocking our full potential.")





;;Keep from folding
  )