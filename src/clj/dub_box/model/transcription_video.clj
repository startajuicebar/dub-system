(ns dub-box.model.transcription-video
  (:require [dub-box.db.core :as db]
            [honey.sql.helpers :as hh]))

(defn create
  [transcription-id video-id & extras]
  (apply db/insert-row! :transcription_video
         {:id (java.util.UUID/randomUUID)
          :transcription_id transcription-id
          :video_id video-id}
         extras))