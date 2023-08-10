(ns dub-box.env
  (:require
   [selmer.parser :as parser]
   [clojure.tools.logging :as log]
   [dub-box.dev-middleware :refer [wrap-dev]]))

(def defaults
  {:init
   (fn []
     (parser/cache-off!)
     (log/info "\n-=[dub-box started successfully using the development profile]=-"))
   :stop
   (fn []
     (log/info "\n-=[dub-box has shut down successfully]=-"))
   :middleware wrap-dev})
