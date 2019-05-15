(defproject nl.openweb/graphql-endpoint "0.1.0-SNAPSHOT"
  :plugins [[lein-modules "0.3.11"]]
  :dependencies [[clojurewerkz/money "1.10.0"]
                 [crypto-password "0.2.1"]
                 [com.stuartsierra/component "0.4.0"]
                 [com.walmartlabs/lacinia-pedestal "0.11.0" :exclusions [com.fasterxml.jackson.core/jackson-core]]
                 [hikari-cp/hikari-cp :version]
                 [nl.openweb/topology :version]
                 [org.clojure/clojure :version]
                 [org.postgresql/postgresql :version]
                 [seancorfield/next.jdbc :version]]
  :main nl.openweb.graphql-endpoint.core
  :profiles {:uberjar {:omit-source  true
                       :aot          [nl.openweb.graphql-endpoint.core]
                       :uberjar-name "ge-docker.jar"}
             :viz {:dependencies [[walmartlabs/system-viz "0.4.0"]]
                   :main nl.openweb.graphql-endpoint.viz
                   }})
