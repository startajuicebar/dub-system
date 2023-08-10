(ns dub-box.model.audio
  (:require [dub-box.db.core :as db]
            [honey.sql.helpers :as hh]
            [dub-box.utils.file :as file-utils]
            [dub-box.utils.av :as av-utils]
            [dub-box.utils.aws :as aws-utils]
            [clojure.java.io :as io]))

(defn get-audios
  []
  (db/get-rows! :audio))

(defn audio-by-id
  [id]
  (db/get-rows! :audio
                :pred [:= :id id]
                :get-single? true))

(defn create
  [values & extras]
  (apply db/insert-row! :audio
         (assoc values :id (java.util.UUID/randomUUID))
         extras))

(defn get-presigned-url
  [id]
  (-> (audio-by-id id)
      :audio/bucket-key
      aws-utils/generate-presigned-url))

(defn extract-audio-from-input-stream
  [stream & {:keys [to-dir
                    format]
             :or {to-dir "tmp"
                  format "flac"}}]
  (file-utils/with-temp-file stream
    (fn [tmp-file]
      (let [output-dir (file-utils/use-or-create-dir to-dir)
            audio-file (av-utils/video->audio (.toString tmp-file) output-dir {:format format})]
        {:bucket_key (.getName audio-file)
         :file-path (.toString audio-file)
         :file audio-file
         :input-stream (io/input-stream audio-file)}))))

(comment

  (get-presigned-url "480082f2-b0a8-459f-918f-8e7f1d391120")


  (.getName (io/file "tmp/CHECKLIST.md"))





;;Keep from folding
  )