(ns dub-box.model.video
  (:require [dub-box.db.core :as db]
            [honey.sql.helpers :as hh]
            [dub-box.utils.aws :as aws-utils]))

(defn get-videos
  []
  (db/get-rows! :video))

(defn video-by-id
  [id]
  (db/get-rows! :video
                :pred [:= :id id]
                :get-single? true))

(defn create
  [values & extras]
  (apply db/insert-row! :video
         (assoc values :id (java.util.UUID/randomUUID))
         extras))

(defn get-presigned-url
  [id]
  (-> (video-by-id id)
      :video/bucket-key
      aws-utils/generate-presigned-url))

(comment
  (get-videos)

  (get-presigned-url "653f8c71-829b-4687-b255-4bc7d15ebec8")

;;Keep from folding
  )