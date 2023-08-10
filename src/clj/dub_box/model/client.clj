(ns dub-box.model.client
  (:require [dub-box.db.core :as db]
            [honey.sql.helpers :as hh]))

(defn get-clients
  []
  (db/get-rows! :client))

(defn client-by-id
  [id]
  (db/get-rows! :client
                :pred [:= :id id]
                :get-single? true))

(comment

  (get-clients)

  (client-by-id "jsh")

;;Keep from folding
  )