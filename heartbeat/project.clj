(defproject nl.openweb/heartbeat "0.1.0-SNAPSHOT"
  :plugins [[lein-modules "0.3.11"]]
  :dependencies [[nl.openweb/topology :version]
                 [org.clojure/clojure :version]
                 [nrepl/nrepl :version]]
  :main nl.openweb.heartbeat.core
  :profiles {:uberjar {:omit-source  true
                       :aot          :all
                       :uberjar-name "hb-docker.jar"}})
