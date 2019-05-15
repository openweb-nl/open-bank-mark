(ns nl.openweb.dev.avro-compile
  (:require [abracad.avro :as avro]
            [clojure.java.io :as io]
            [clojure.edn :as edn])
  (:import (org.apache.avro Schema)
           (org.apache.avro.compiler.specific SpecificCompiler SpecificCompiler$FieldVisibility)
           (java.io File)
           (org.apache.avro.generic GenericData$StringType))
  (:gen-class))

(defn fill-fields
  [sub-types field]
  (if (keyword? field)
    {:name (name field) :type (field sub-types)}
    field))

(defn schema-reducer
  [sub-types m name fields]
  (conj m (avro/parse-schema (-> {}
                                 (assoc :type :record)
                                 (assoc :name name)
                                 (assoc :namespace "nl.openweb.data")
                                 (assoc :fields (mapv #(fill-fields sub-types %) fields))))))

(defn schemas
  []
  (if-let [clj-schemas (-> (io/resource "schemas.edn")
                           slurp
                           edn/read-string)]
    (reduce-kv (partial schema-reducer (:types clj-schemas)) [] (:records clj-schemas))))

(doseq [^Schema schema (schemas)]
  (let [compiler (SpecificCompiler. schema)
        _ (doto compiler
            (.setFieldVisibility SpecificCompiler$FieldVisibility/PRIVATE)
            (.setStringType GenericData$StringType/String))
        java-target-path "target/main/java"
        file (File. "")
        dest (File. ^String java-target-path)]
    (.compileToDestination compiler file dest)))
