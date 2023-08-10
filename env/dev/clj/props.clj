(ns props
  (:require [cprop.tools :as t]
            [cprop.source :refer [from-stream]]
            [dub-box.config :refer [env]]
            [clojure.pprint :as pprint]))

(defn print-env-vars
  []
  (print (slurp (t/map->env-file (from-stream "dev-config.edn")))))

(comment

  (print-env-vars)

;;Keep from folding
  )