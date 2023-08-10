(ns dub-box.model.transcription-audio
  (:require [dub-box.db.core :as db]
            [honey.sql.helpers :as hh]))

(defn create
  [transcription-id audio-id & extras]
  (apply db/insert-row! :transcription_audio
         {:id (java.util.UUID/randomUUID)
          :transcription_id transcription-id
          :audio_id audio-id}
         extras))