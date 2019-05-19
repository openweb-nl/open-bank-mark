(ns nl.openweb.graphql-endpoint.system
  (:require
    [com.stuartsierra.component :as component]
    [nl.openweb.graphql-endpoint.account-creation-service :as account-creation-service]
    [nl.openweb.graphql-endpoint.kafka-producer :as kafka-producer]
    [nl.openweb.graphql-endpoint.money-transfer-service :as money-transfer-service]
    [nl.openweb.graphql-endpoint.postgres-db :as db]
    [nl.openweb.graphql-endpoint.schema :as schema]
    [nl.openweb.graphql-endpoint.server :as server]
    [nl.openweb.graphql-endpoint.transaction-service :as transaction-service]))

(defn new-system
  []
  (merge (component/system-map)
         (server/new-server)
         (schema/new-schema-provider)
         (transaction-service/new-service)
         (account-creation-service/new-service)
         (money-transfer-service/new-service)
         (db/new-db)
         (kafka-producer/new-producer)))
