(defproject nl.openweb/synchronizer "0.1.0-SNAPSHOT"
            :plugins [[lein-modules "0.3.11"]]
            :dependencies [[clj-http "3.10.0" :exclusions [commons-logging]]
                           [nl.openweb/topology :version]
                           [org.clojure/clojure :version]
                           [org.clojure/data.json :version]
                           [org.slf4j/jcl-over-slf4j "1.7.26"]]
            :main nl.openweb.synchronizer.core
            :profiles {:uberjar {:omit-source  true
                                 :aot          :all
                                 :uberjar-name "syn-docker.jar"}})
