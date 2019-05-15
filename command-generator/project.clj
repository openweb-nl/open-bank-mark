(defproject nl.openweb/command-generator "0.1.0-SNAPSHOT"
  :plugins [[lein-modules "0.3.11"]]
  :dependencies [[nl.openweb/topology :version]
                 [org.clojure/clojure :version]]
  :main nl.openweb.command-generator.core
  :profiles {:uberjar {:omit-source  true
                       :aot          :all
                       :uberjar-name "cg-docker.jar"}})
