(ns nl.openweb.test.process
  (:require [clj-docker-client.core :as docker]))

(defonce docker-conn (atom nil))

(defn init
  []
  (reset! docker-conn (docker/connect))
  (docker/stats @docker-conn "db")
  (docker/stats @docker-conn "command-handler")
  (docker/stats @docker-conn "kafka-1")
  (docker/stats @docker-conn "kafka-2")
  (docker/stats @docker-conn "kafka-3"))

(defn get-info
  []
  (let [db-stats (docker/stats @docker-conn "db")
        ch-stats (docker/stats @docker-conn "command-handler")
        k1-stats (docker/stats @docker-conn "kafka-1")
        k2-stats (docker/stats @docker-conn "kafka-2")
        k3-stats (docker/stats @docker-conn "kafka-3")]
    [(:cpu-pct db-stats)
     (:mem-mib db-stats)
     (:cpu-pct ch-stats)
     (:mem-mib ch-stats)
     (+ (:cpu-pct k1-stats) (:cpu-pct k2-stats) (:cpu-pct k3-stats))
     (+ (:mem-mib k1-stats) (:mem-mib k2-stats) (:mem-mib k3-stats))]
    ))

(defn close
  []
  (docker/disconnect @docker-conn)
  (reset! docker-conn nil))