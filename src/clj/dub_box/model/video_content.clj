(ns dub-box.model.video-content
  (:require [dub-box.db.core :as db]
            [honey.sql.helpers :as hh]))

(defn create
  [values & extras]

  (apply db/insert-row! :video_content
         (assoc values :id (java.util.UUID/randomUUID))
         extras))

(defn get-by-id
  [id]
  (db/get-rows! :video_content
                :pred [:= :id id]
                :get-single? true))

(defn get-transcriptions
  [video-content-id]
  (db/get-rows! :transcription
                :pred [:= :video_content_id video-content-id]))

(defn get-first-native-transcription
  [video-content-id]
  (db/get-rows! :transcription
                :get-single? true
                :pred [:and [:= :video_content_id video-content-id]
                       [:= :native true]]))

(comment

  (get-transcriptions "5fc04a96-f6e6-49eb-a032-70b5c343c956")

;;Keep from folding
  )