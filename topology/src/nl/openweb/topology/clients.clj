(ns nl.openweb.topology.clients
  (:require [clj-time.local :as time-local]
            [nl.openweb.topology.core :as core]
            [nl.openweb.topology.value-generator :as vg])
  (:import (io.confluent.kafka.serializers KafkaAvroSerializer AbstractKafkaAvroSerDeConfig KafkaAvroDeserializer KafkaAvroDeserializerConfig)
           (java.util Properties)
           (org.apache.kafka.clients.producer ProducerRecord KafkaProducer ProducerConfig)
           (org.apache.kafka.common.serialization StringSerializer StringDeserializer)
           (org.apache.kafka.clients.consumer ConsumerConfig KafkaConsumer ConsumerRecords ConsumerRecord)
           (org.apache.avro.specific SpecificRecord))
  (:gen-class))

(def brokers (or (System/getenv "KAFKA_BROKERS") "localhost:9092"))
(def schema-url (or (System/getenv "SCHEMA_REGISTRY_URL") "http://localhost:8081"))

(defn get-key
  [topic-name value]
  (if-let [key (nth (core/get-topic topic-name) 4)]
    (cond
      (= key :id) (-> (.getId value)
                      .bytes
                      vg/bytes->uuid
                      .toString)
      (= key :now) (str (time-local/to-local-date-time (time-local/local-now)))
      (= key :iban) (.getIban value)
      :else nil)))

(defn produce
  [^KafkaProducer producer ^String topic-name ^SpecificRecord record]
  (if-let [pr (ProducerRecord. topic-name (get-key topic-name record) record)]
    (.send producer pr)))

(defn get-producer
  [client-id & {:keys [config]}]
  (let [properties (Properties.)]
    (doto properties
      (.put ProducerConfig/BOOTSTRAP_SERVERS_CONFIG brokers)
      (.put ProducerConfig/CLIENT_ID_CONFIG client-id)
      (.put ProducerConfig/KEY_SERIALIZER_CLASS_CONFIG (.getName StringSerializer))
      (.put ProducerConfig/VALUE_SERIALIZER_CLASS_CONFIG (.getName KafkaAvroSerializer))
      (.put ProducerConfig/LINGER_MS_CONFIG (.intValue 100))
      (.put ProducerConfig/ACKS_CONFIG "all")
      (.put AbstractKafkaAvroSerDeConfig/SCHEMA_REGISTRY_URL_CONFIG schema-url)
      (.put AbstractKafkaAvroSerDeConfig/AUTO_REGISTER_SCHEMAS false)
      #(doseq [[prop-name prop-val] config] (.put % prop-name prop-val)))
    (KafkaProducer. properties)))

(defn poll-execute
  [consumer function]
  (let [^ConsumerRecords records (.poll consumer 100)]
    (doseq [^ConsumerRecord record records] (function record))))

(defn consumer-loop [keep-running consumer function]
  (if @keep-running
    (do
      (poll-execute consumer function)
      (recur keep-running consumer function))
    (.close consumer)))

(defn get-consumer
  [client-id group-id & {:keys [config]}]
  (let [properties (Properties.)]
    (doto properties
      (.put ConsumerConfig/BOOTSTRAP_SERVERS_CONFIG brokers)
      (.put ConsumerConfig/CLIENT_ID_CONFIG client-id)
      (.put ConsumerConfig/GROUP_ID_CONFIG group-id)
      (.put ConsumerConfig/AUTO_OFFSET_RESET_CONFIG "earliest")
      (.put ConsumerConfig/MAX_POLL_RECORDS_CONFIG (int 100))
      (.put ConsumerConfig/KEY_DESERIALIZER_CLASS_CONFIG (.getName StringDeserializer))
      (.put ConsumerConfig/VALUE_DESERIALIZER_CLASS_CONFIG (.getName KafkaAvroDeserializer))
      (.put KafkaAvroDeserializerConfig/SPECIFIC_AVRO_READER_CONFIG "true")
      (.put AbstractKafkaAvroSerDeConfig/SCHEMA_REGISTRY_URL_CONFIG schema-url)
      #(doseq [[prop-name prop-val] config] (.put % prop-name prop-val)))
    (KafkaConsumer. properties)))

(defn consume
  [client-id group-id topic function & {:keys [config]}]
  (let [keep-running (atom true)
        consumer (if config (get-consumer client-id group-id config) (get-consumer client-id group-id))]
    (if (vector? topic)
      (.subscribe consumer topic)
      (.subscribe consumer [topic]))
    (future (consumer-loop keep-running consumer function))
    #(reset! keep-running false)))
