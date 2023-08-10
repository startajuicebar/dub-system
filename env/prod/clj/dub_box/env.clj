(ns dub-box.env
  (:require [clojure.tools.logging :as log]))

(def defaults
  {:init
   (fn []
     (log/info "\n-=[dub-box started successfully]=-"))
   :stop
   (fn []
     (log/info "\n-=[dub-box has shut down successfully]=-"))
   :middleware identity})
