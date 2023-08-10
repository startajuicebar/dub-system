(ns dub-box.core
  (:require [clojure.tools.cli :refer [parse-opts]]
            [clojure.tools.logging :as log]
            [dub-box.config :refer [env]]
            [dub-box.db.core :as db]
            [dub-box.handler :as handler]
            [luminus.http-server :as http]
            [mount.core :as mount] ;; Call all requires here
            )
  (:gen-class))

;; log uncaught exceptions in threads
(Thread/setDefaultUncaughtExceptionHandler
 (reify Thread$UncaughtExceptionHandler
   (uncaughtException [_ thread ex]
     (log/error {:what :uncaught-exception
                 :exception ex
                 :where (str "Uncaught exception on" (.getName thread))}))))

(def cli-options
  [["-p" "--port PORT" "Port number"
    :parse-fn #(Integer/parseInt %)]])

(mount/defstate ^{:on-reload :noop} http-server
  :start
  (http/start
   (-> env
       (update :io-threads #(or % (* 2 (.availableProcessors (Runtime/getRuntime)))))
       (assoc  :handler (handler/app))
       (update :port #(or (-> env :options :port) %))
       (select-keys [:handler :host :port])))
  :stop
  (http/stop http-server))

(defn stop-app []
  (doseq [component (:stopped (mount/stop))]
    (log/info component "stopped"))
  (shutdown-agents))

(defn start-app [args]
  (doseq [component (-> args
                        (parse-opts cli-options)
                        mount/start-with-args
                        :started)]
    (log/info component "started"))
  (.addShutdownHook (Runtime/getRuntime) (Thread. stop-app)))

(defn -main [& args]
  (mount/start #'dub-box.config/env)
  (start-app args))