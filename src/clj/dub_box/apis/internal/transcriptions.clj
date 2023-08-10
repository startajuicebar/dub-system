(ns dub-box.apis.internal.transcriptions
  (:require [dub-box.model.transcription :as transcription-model]
            [dub-box.model.video-content :as video-content-model]
            [dub-box.utils.ffmpeg :as ffmpeg]
            [dub-box.model.utterance :as utterance-model]
            [dub-box.utils.audio :as audio-utils]))

(defn create-transcription-handler
  [{:keys [body-params]}]

  (let [{:keys [video-content-id transcription-data]} body-params
        {:keys [name native language]} transcription-data
        native-transcription-record (video-content-model/get-first-native-transcription video-content-id)]

    {:status 200
     :body {:result
            (transcription-model/clone-to-target-language!
             (:transcription/id native-transcription-record)
             language
             {:native? native
              :name name})}}))

(defn- *generate-audio-deps-audio!
  [utterances]
  (doseq [utterance utterances]
    (utterance-model/generate-audio! (:utterance/id utterance))))

(defn generate-missing-deps-audio!
  [transcription-id]
  (let [utterances-missing-audio (transcription-model/utterances-missing-audio transcription-id)
        missing-utterance-audio? (seq utterances-missing-audio)]

    (when missing-utterance-audio?
      (*generate-audio-deps-audio! utterances-missing-audio))
    transcription-id))

(defn generate-audio-deps-audio!
  [transcription-id]
  (let [utterances (transcription-model/get-utterances-by-id transcription-id)]
    (doseq [utterance utterances]
      (utterance-model/generate-audio! (:utterance/id utterance)))
    transcription-id))

(defn generate-audio!
  [transcription-id]
  (let [utterance-records (transcription-model/get-utterances-by-id transcription-id)
        utterance-ids (map :utterance/id utterance-records)
        signed-audio-urls (map utterance-model/get-signed-audio-url utterance-ids)]

    (ffmpeg/concat-audio signed-audio-urls (str "resources/" transcription-id ".mp3"))))

(comment

  (->
   (generate-audio-deps-audio! "33a209d9-d62b-4419-84dc-5072419700ea")
   (generate-audio!)
   audio-utils/play-audio)

  (generate-audio! "e75286e6-295e-485a-9ca5-c4a4aff13447")

  (generate-missing-deps-audio! "6ca6ca20-0df8-43c3-a140-1da2972f8470")
  (generate-missing-deps-audio! "33a209d9-d62b-4419-84dc-5072419700ea")

  (generate-audio! "33a209d9-d62b-4419-84dc-5072419700ea")
  (generate-audio! "33a209d9-d62b-4419-84dc-5072419700ea")
  (-> (generate-audio! "e75286e6-295e-485a-9ca5-c4a4aff13447")
      audio-utils/play-audio)


;;Keep from folding
  )

(def routes
  ["/transcriptions"
   {:swagger {:tags ["transcriptions"]}}

   [""
    {:post {:summary "Create a new transcription"
            :responses {200 {:body {:result map?}}
                        400 {:body {:error string?}}}
            :handler create-transcription-handler}}]])

