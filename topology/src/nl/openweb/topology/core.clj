(ns nl.openweb.topology.core
  (:require [clojure.edn :as edn]
            [clojure.java.io :as io])
  (:import (clojure.lang Reflector))
  (:gen-class))

(defonce topology
         (-> (io/resource "topology.edn")
             slurp
             edn/read-string))

(defn get-topic
  [topic-name]
  (get topology topic-name))

(defn get-schema
  [topic-name]
  (if-let [key (nth (get-topic topic-name) 2)]
    (Reflector/getStaticField ^String (str "nl.openweb.data." (name key)) "SCHEMA$")))
