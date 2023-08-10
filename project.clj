(defproject dub-box "0.0.1"
  :description "Dub Box"

  :dependencies [[javazoom/jlayer "1.0.1"]
                 [net.clojars.wkok/openai-clojure "0.8.0"]
                 [clj-python/libpython-clj "2.024"]
                 [com.rpl/specter "1.1.4"]
                 [ch.qos.logback/logback-classic "1.2.3"]
                 [clojure.java-time "1.2.0"]
                 [org.clojure/tools.trace "0.7.11"]
                 [clojure-interop/java.security "1.0.5"]
                 [com.auth0/java-jwt "3.18.2"]
                 [hiccup "1.0.5"]
                 [conman "0.9.1"]
                 [ovotech/ring-jwt "2.2.1"]
                 [cprop "0.1.17"]
                 [expound "0.8.9"]
                 [tick/tick "0.5.0"]
                 [funcool/struct "1.4.0"]
                 [funcool/cuerdas "2.2.1"]
                 [pandect "1.0.1"]
                 [luminus-transit "0.1.2"]
                 [luminus-undertow "0.1.10"]
                 [metosin/potpuri "0.5.3"]
                 [luminus/ring-ttl-session "0.3.3"]
                 [markdown-clj "1.10.5"]
                 [metosin/muuntaja "0.6.8"]
                 [metosin/reitit "0.7.0-alpha5"]
                 [com.github.seancorfield/honeysql "2.4.1045"]
                 [org.postgresql/postgresql "42.6.0"]
                 [amazonica "0.3.165"]
                 [luissantos/ffclj "399c4ea9a4e2ab5fca3dde9c7e935f7ddd488191"]
                 [org.clj-commons/byte-streams "0.3.4"]
                 [com.knuddels/jtokkit "0.6.1"]
                 [com.thedeanda/lorem "2.1"]
                 [metosin/ring-http-response "0.9.2"]
                 [clj-http "3.12.0"]
                 [camel-snake-kebab "0.4.2"]
                 [metosin/spec-tools "0.10.5"]
                 [com.taoensso/timbre "6.1.0"]
                 [com.taoensso/encore "3.59.0"]
                 [org.clojure/data.csv "1.0.0"]
                 [org.clojure/core.async "1.3.618"]
                 [org.clj-commons/hickory "0.7.3"]
                 [metosin/reitit-dev "0.7.0-alpha5"]
                 [ring-logger "1.0.1"]
                 [exoscale/ex "0.3.18"]
                 [metosin/sieppari "0.0.0-alpha13"]
                 [metosin/malli "0.11.0"]
                 [mount "0.1.16"]
                 [org.clojure/clojure "1.11.1"]
                 [org.clojure/tools.cli "1.0.214"]
                 [org.clojure/tools.logging "1.1.0"]
                 [org.webjars/webjars-locator "0.40"]
                 [org.webjars/webjars-locator-jboss-vfs "0.1.0"]
                 [ring-webjars "0.2.0"]
                 [ring/ring-core "1.9.1"]
                 [ring/ring-codec "1.1.3"]
                 [ring/ring-defaults "0.3.2"]
                 [selmer "1.12.33"]]

  :min-lein-version "2.0.0"
  :source-paths ["src/clj/"]
  :java-source-paths ["src/java"]

  :test-paths ["test/clj/"]
  :resource-paths ["resources"]
  :target-path "target/%s/"
  :main ^:skip-aot dub-box.core

  :plugins [[reifyhealth/lein-git-down "0.4.1"]
            [lein-cljfmt "0.7.0"]]

  :middleware [lein-git-down.plugin/inject-properties]

  :repositories [["central" "https://repo1.maven.org/maven2/"]
                 ["clojars" "https://repo.clojars.org/"]
                 ["jitpack" "https://jitpack.io"]
                 ["public-github" {:url "git://github.com"}]]

  :profiles {;;  The order of these matter, the portal profile must come first as it's needed by future profiles
             :dev           [:portal :dev/base]

             :kaocha {:dependencies [[lambdaisland/kaocha "1.80.1274"]]}

             :portal {:dependencies [[djblue/portal "RELEASE"]]
                      :repl-options
                      {:nrepl-middleware [portal.nrepl/wrap-portal]}}

             :uberjar {:omit-source true
                       :aot :all
                       :uberjar-name "dub-box.jar"
                       :source-paths ["env/prod/clj"]
                       :resource-paths ["env/prod/resources"]}

             :dev/base  {:jvm-opts ["-Dconf=dev-config.edn"]
                         :dependencies [[pjstadig/humane-test-output "0.11.0"]
                                        [com.wsscode/pathom-viz-connector "2021.04.20"]
                                        [com.nextjournal/beholder "1.0.2"]
                                        [prone "2020-01-17"]
                                        [ring/ring-devel "1.9.6"]
                                        [org.clojure/tools.namespace "1.4.2"]
                                        [ring-refresh "0.1.3"]
                                        [ring/ring-mock "0.4.0"]
                                        [nrepl "0.9.0"]]

                         :plugins      [[com.jakemccrary/lein-test-refresh "0.25.0"]
                                        [jonase/eastwood "1.3.0"]]

                         :source-paths ["env/dev/clj"]
                         :resource-paths ["env/dev/resources"]
                         :repl-options {:init-ns user
                                        :welcome (println "Welcome to the magical world of the repl!")
                                        :port 7070
                                        :timeout 120000}
                         :injections [(require 'pjstadig.humane-test-output)
                                      (pjstadig.humane-test-output/activate!)]}
             :test {:jvm-opts ["-Dconf=test-config.edn"]
                    :resource-paths ["env/test/resources"]}}

  :aliases {"kaocha" ["with-profile" "+kaocha" "run" "-m" "kaocha.runner"]})
