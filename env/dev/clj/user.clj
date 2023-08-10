(ns user
  "Userspace functions you can run by default in your local REPL."
  (:require [clojure.java.classpath :as classpath]
            [clojure.pprint]
            [clojure.spec.alpha :as s]
            [clojure.tools.logging :as log]
            [clojure.tools.namespace.find :as ns-find]
            [cprop.tools :as t]
            [expound.alpha :as expound]
            [dub-box.config :refer [env]]
            [dub-box.core]
            [mount.core :as mount]
            [next.jdbc.specs :as specs]
            [nrepl-server :as nrepl]
            [portal.api :as p]
            [props])
  (:import java.util.jar.JarFile))

(specs/instrument)

(alter-var-root #'s/*explain-out* (constantly expound/printer))

(add-tap (bound-fn* clojure.pprint/pprint))

(mount/defstate ^{:on-reload :noop} repl-server
  :start (when (env :nrepl-port)
           (log/info "Starting repl server")
           (nrepl/start {:bind (env :nrepl-bind)
                         :port (env :nrepl-port)}))

  :stop (when repl-server
          (log/info "Stopping repl server")
          (nrepl/stop repl-server)))

(mount/defstate ^{:on-reload :noop} portal
  :start (let [p (p/open {:launcher :vs-code
                          :theme :portal.colors/gruvbox})]
           (log/info "Opening portal")
           (add-tap #'p/submit)
           p)

  :stop (when portal
          (log/info "Stopping portal")
          (remove-tap #'p/submit)
          (p/close)))

(defn start
  "Starts application.
  You'll usually want to run this on startup."
  []
  (log/info "Starting application")
  (mount/start-without #'user/repl-server))

(defn stop
  "Stops application."
  []
  (log/info "Stopping application")
  (mount/stop-except #'user/repl-server))

(defn restart
  "Restarts application."
  []
  (log/info "Restarting application")
  (stop)
  (start))

(defn clear-portal
  []
  (p/clear))

(defn close-portal
  []
  (p/clear)
  (p/close))

(defn find-namespaces-in-jar [jar-file]
  (ns-find/find-namespaces-in-jarfile jar-file))





