(ns nl.openweb.heartbeat.core
  (:require
    [nl.openweb.topology.clients :as clients]
    [nrepl.server :refer [start-server]])
  (:import (nl.openweb.data Heartbeat))
  (:gen-class))

(def topic (or (System/getenv "KAFKA_HEARTBEAT_TOPIC") "heartbeat"))

(def counter (atom 0))
(def batch-size (atom 0))
(def sleep-time (atom 1000))
(def continue (atom true))

(defn p-loop
  [producer]
  (let [b-size @batch-size
        c @counter]
    (doseq [s (range b-size)]
      (clients/produce producer topic (Heartbeat. (+ s c))))
    (swap! counter #(+ % b-size)))
  (Thread/sleep @sleep-time)
  (if @continue (recur producer)))

(defn -main
  []
  (start-server :bind "0.0.0.0" :port 17888)
  (p-loop (clients/get-producer "heartbeat")))