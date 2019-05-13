(ns nl.openweb.test.process
  (:require [clj-docker-client.core :as docker]))

(defonce docker-conn (atom nil))

(defn init
  []
  (reset! docker-conn (docker/connect))
  (docker/stats @docker-conn "db")
  (docker/stats @docker-conn "command-handler")
  (docker/stats @docker-conn "kafka")
  (docker/stats @docker-conn "graphql-endpoint"))

(defn get-info
  []
  (let [db-stats (docker/stats @docker-conn "db")
        ch-stats (docker/stats @docker-conn "command-handler")
        kb-stats (docker/stats @docker-conn "kafka")
        ge-stats (docker/stats @docker-conn "graphql-endpoint")]
    [(:cpu-pct db-stats)
     (:mem-mib db-stats)
     (:cpu-pct ch-stats)
     (:mem-mib ch-stats)
     (:cpu-pct kb-stats)
     (:mem-mib kb-stats)
     (:cpu-pct ge-stats)
     (:mem-mib ge-stats)]))

(defn close
  []
  (docker/disconnect @docker-conn)
  (reset! docker-conn nil))