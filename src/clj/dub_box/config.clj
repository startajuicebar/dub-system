(ns dub-box.config
  (:require
   [cprop.core :refer [load-config]]
   [cprop.source :as source]
   [clojure.tools.logging :as log]
   [mount.core :refer [args defstate]]))

(defn reload-config
  []
  (log/info "Reloading config")
  (load-config
   :merge
   [(args)
    (source/from-system-props)
    (source/from-env)]))

(defstate env
  :start
  (reload-config))

(comment


  (reload-config)
;;Keep from folding
  )

