(defproject nl.openweb/command-handler "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :license {:name "Eclipse Public License"
            :url  "http://www.eclipse.org/legal/epl-v10.html"}
  :plugins [[lein-modules "0.3.11"]]
  :dependencies [[nl.openweb/topology :version]
                 [org.clojure/clojure :version]
                 [org.postgresql/postgresql :version]
                 [seancorfield/next.jdbc :version]
                 [hikari-cp/hikari-cp :version]]
  :main nl.openweb.command-handler.core
  :profiles {:uberjar {:omit-source  true
                       :aot          :all
                       :uberjar-name "ch-docker.jar"}})
