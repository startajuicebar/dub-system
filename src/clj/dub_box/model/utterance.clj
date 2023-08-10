(ns dub-box.model.utterance
  (:require [cheshire.core :refer [generate-string parse-string]]
            [clojure.java.io :as io]
            [dub-box.db.core :as db]
            [dub-box.utils.audio :as audio-utils]
            [dub-box.utils.av :as av-utils]
            [dub-box.utils.aws :as aws-utils]
            [dub-box.utils.elevan-labs :as elevan-utils]
            [dub-box.utils.file :as file-utils]
            [honey.sql.helpers :as hh]))

(defn get-by-id
  [id]
  (db/get-rows! :utterance
                :pred [:= :id id]
                :get-single? true))

(defn create
  [values & extras]
  (apply db/insert-row! :utterance
         (assoc values :id (java.util.UUID/randomUUID))
         extras))

(defn prep-for-translation
  [id]
  (let [u (get-by-id id)]
    {:text (:utterance/transcript u)
     :range [(:utterance/start-time u) (:utterance/end-time u)]}))

(defn generate-audio!
  [id]
  (println (str "Start generating audio for utterance " id))
  (let [u (get-by-id id)
        audio-file (elevan-utils/generate-audio (:utterance/transcript u))
        upload-result (aws-utils/upload-file audio-file)
        audio-record (db/insert-row! :audio
                                     {:id (java.util.UUID/randomUUID)
                                      :bucket_key (:key upload-result)})
        utterance-audio-record (db/insert-row! :utterance_audio
                                               {:id (java.util.UUID/randomUUID)
                                                :audio_id (:audio/id audio-record)
                                                :utterance_id id})]

    {:audio audio-record
     :utterance-audio utterance-audio-record}))

(defn get-audio-by-id
  [utterance-id]
  (-> (hh/select-distinct :a.*)
      (hh/from [:audio :a])
      (hh/join [:utterance_audio :ua] [:= :a.id :ua.audio_id])
      (hh/where [:= :ua.utterance_id utterance-id])
      db/execute-one!))

(defn get-signed-audio-url
  [utterance-id]
  (let [audio-record (get-audio-by-id utterance-id)
        key (:audio/bucket-key audio-record)]
    (when key
      (aws-utils/generate-presigned-url key))))

(defn get-audio-bucket-key-by-id
  [utterance-id]
  (-> (get-audio-by-id utterance-id)
      :audio/bucket-key))

(defn play-audio!
  [utterance-id]
  (let [bucket-key (get-audio-bucket-key-by-id utterance-id)
        has-file? (file-utils/has-file? (str "resources/" bucket-key))]

    (if has-file?
      (audio-utils/play-audio (str "resources/" bucket-key))
      (-> (file-utils/download-file (get-signed-audio-url utterance-id)
                                    (str "resources/" bucket-key))
          audio-utils/play-audio))))