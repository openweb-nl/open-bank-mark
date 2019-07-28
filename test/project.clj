(defproject nl.openweb/test "0.1.0-SNAPSHOT"
            :plugins [[lein-modules "0.3.11"]]
            :dependencies [[com.fasterxml.jackson.core/jackson-annotations :version]
                           [com.fasterxml.jackson.core/jackson-core :version]
                           [com.fasterxml.jackson.core/jackson-databind :version]
                           [etaoin "0.3.5"]
                           [kixi/stats "0.5.0"]
                           [lispyclouds/clj-docker-client "0.2.3"]
                           [metasoarous/oz "1.6.0-alpha2"]
                           [nrepl :version]
                           [org.apache.httpcomponents/httpasyncclient "4.1.4"]
                           [org.apache.httpcomponents/httpclient-cache "4.5.8"]
                           [org.clojure/clojure :version]]
            :main nl.openweb.test.core
            :profiles {:uberjar {:omit-source  true
                                 :aot          :all
                                 :uberjar-name "test.jar"}})
