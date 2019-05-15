(ns nl.openweb.synchronizer.admin
  (:require [clojure.tools.logging :as log])
  (:import (java.util Properties ArrayList)
           (org.apache.kafka.clients.admin AdminClient AdminClientConfig NewTopic CreateTopicsResult DeleteTopicsResult)))

(def brokers (or (System/getenv "KAFKA_BROKERS") "localhost:9092"))
(def admin (atom nil))

(defn create-new-topic
  [topic options nodes]
  (let [[numPartitions replicationFactor _ config] options
        new-topic (NewTopic. topic numPartitions (min replicationFactor nodes))
        string-config (reduce-kv #(assoc %1 (name %2) %3) {} config)]
    (.configs new-topic string-config)))

(defn describe-cluster-nodes
  []
  (if-let [a-i @admin]
    (.get (.nodes (.describeCluster a-i)))))

(defn complete-create-topic
  [^CreateTopicsResult result]
  (doseq [[topic kafka-future] (.values result)]
    (.get kafka-future)
    (log/debug "topic" topic "created")))

(defn create-topics
  [new-topics nodes]
  (if-let [a-i @admin]
    (let [java-list (ArrayList.)
          _ (doseq [[topic options] new-topics] (.add java-list (create-new-topic topic options nodes)))
          result (.createTopics a-i java-list)]
      (log/debug "creating topics" new-topics)
      (complete-create-topic result))))

(defn complete-delete-topic
  [^DeleteTopicsResult result]
  (doseq [[topic kafka-future] (.values result)]
    (.get kafka-future)
    (log/debug "topic" topic "deleted")))

(defn delete-topics
  [topics]
  (if-let [a-i @admin]
    (let [result (.deleteTopics a-i topics)]
      (log/debug "deleted topics" topics)
      (complete-delete-topic result))))

(defn list-topics
  []
  (if-let [a-i @admin]
    (.get (.names (.listTopics a-i)))))

(defn start
  []
  (if @admin (.close @admin))
  (let [properties (Properties.)]
    (.put properties AdminClientConfig/BOOTSTRAP_SERVERS_CONFIG brokers)
    (.put properties AdminClientConfig/CLIENT_ID_CONFIG "synchronizer")
    (reset! admin (AdminClient/create properties))))