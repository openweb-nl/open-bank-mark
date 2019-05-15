(ns nl.openweb.synchronizer.schema
  (:require [clojure.data.json :as json]
            [clojure.tools.logging :as log]
            [clj-http.client :as client]
            [nl.openweb.topology.core :as topology]))

(def schema-registry-url (or (System/getenv "SCHEMA_REGISTRY_URL") "http://localhost:8081"))

(defn set-schema
  [topic]
  (if-let [schema (topology/get-schema topic)]
    (let [result (client/post (str schema-registry-url "/subjects/" topic "-value/versions")
                              {:body           (json/write-str {"schema" (.toString schema)})
                               :headers        {"Content-Type" "application/vnd.schemaregistry.v1+json"}
                               :content-type   :json
                               :socket-timeout 1000             ;; in milliseconds
                               :conn-timeout   1000             ;; in milliseconds
                               :accept         :json})]
      (log/debug "set schema" schema "for topic" topic "with result" result))))
