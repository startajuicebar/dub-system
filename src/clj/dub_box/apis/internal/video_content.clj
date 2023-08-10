(ns dub-box.apis.internal.video-content
  (:require [byte-streams :as bs]
            [dub-box.db.core :as db]
            [dub-box.model.audio :as audio-model]
            [dub-box.model.transcription :as transcription-model]
            [dub-box.model.transcription-audio :as transcription-audio-model]
            [dub-box.model.transcription-video :as transcription-video-model]
            [dub-box.model.video :as video-model]
            [dub-box.model.video-content :as video-content-model]
            [dub-box.utils.aws :as aws-utils]
            [dub-box.utils.file :refer [get-extention-by-type]]
            [dub-box.utils.mux :as mux-utils]
            [next.jdbc :as jdbc]
            [potpuri.core]
            [reitit.coercion.spec])
  (:import [java.io ByteArrayInputStream]))

(defn build-tree
  [name client-id uploaded-source-video uploaded-source-audio mux-asset]
  (jdbc/with-transaction [tx db/conn]
    (let [video-record (video-model/create {:bucket_key (:key uploaded-source-video)
                                            :mux_asset_id (-> mux-asset :data :id)
                                            :mux_playback_id (-> mux-asset :data :playback_ids first :id)} {:connection tx})
          audio-record (audio-model/create {:bucket_key (:key uploaded-source-audio)} {:connection tx})

          video-content-record (video-content-model/create {:name name
                                                            :client_id client-id
                                                            :source_video_id (:video/id video-record)} {:connection tx})

          transcription-record (transcription-model/create
                                (:video-content/id video-content-record)
                                {:native? true
                                 :language "en"
                                 :connection tx})

          transcription-video-record (transcription-video-model/create
                                      (:transcription/id transcription-record)
                                      (:video/id video-record)
                                      {:connection tx})
          transcription-audio-record (transcription-audio-model/create
                                      (:transcription/id transcription-record)
                                      (:audio/id audio-record)
                                      {:connection tx})]
      {:video-content video-content-record
       :video video-record
       :transcription transcription-record
       :transcription-video transcription-video-record
       :transcription-audio transcription-audio-record})))

(defn create-new-video-content
  [name client-id key content]

  (let [content-bytes (bs/to-byte-array content)

        uploaded-source-video (aws-utils/upload-stream (ByteArrayInputStream. content-bytes) key)

        mux-asset (mux-utils/create-asset! (aws-utils/generate-presigned-url key))

        audio-input-stream (audio-model/extract-audio-from-input-stream (ByteArrayInputStream. content-bytes))

        uploaded-source-audio (aws-utils/upload-stream (:input-stream audio-input-stream) (:bucket_key audio-input-stream))

        process-result (build-tree name client-id uploaded-source-video uploaded-source-audio mux-asset)

        transcribe-result (transcription-model/transcribe! (-> process-result :transcription :transcription/id))]

    (merge process-result transcribe-result)))

(defn upload-video-handler
  [{:as request
    :keys [params
           content-type
           body]}]

  (let [extention (get-extention-by-type content-type)
        client-id (:client-id params)
        name (or (:name params) "Untitled")
        key (str (java.util.UUID/randomUUID) "." extention)]

    {:status 200
     :body {:result (create-new-video-content name client-id key body)}}))

(def routes
  ["/video-content"
   {:swagger {:tags ["video-content"]}}

   [""
    {:post {:summary "Upload video-content"
            :responses {200 {:body {:result map?}}
                        400 {:body {:error string?}}}
            :handler upload-video-handler}}]])

(comment

  ;; Keep from folding
  )