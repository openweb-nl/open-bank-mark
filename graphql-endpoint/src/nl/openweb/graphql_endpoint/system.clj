(ns nl.openweb.graphql-endpoint.system
  (:require
    [com.stuartsierra.component :as component]
    [nl.openweb.graphql-endpoint.account-creation-service :as a-service]
    [nl.openweb.graphql-endpoint.kafka-producer :as kafka-p]
    [nl.openweb.graphql-endpoint.money-transfer-service :as m-service]
    [nl.openweb.graphql-endpoint.postgres-db :as db]
    [nl.openweb.graphql-endpoint.schema :as schema]
    [nl.openweb.graphql-endpoint.server :as server]
    [nl.openweb.graphql-endpoint.transaction-service :as t-service]))

(defn new-system
  []
  (merge (component/system-map)
         (server/new-server)
         (schema/new-schema-provider)
         (t-service/new-service)
         (a-service/new-service)
         (m-service/new-service)
         (db/new-db)
         (kafka-p/new-producer)))
