(ns nl.openweb.graphql-endpoint.postgres-db
  (:require [com.stuartsierra.component :as component]
            [hikari-cp.core :as h]
            [nl.openweb.topology.clients :as clients]))

(def db-port (read-string (or (System/getenv "DB_PORT") "5432")))
(def db-hostname (or (System/getenv "DB_HOSTNAME") "localhost"))
(def db-password (or (System/getenv "DB_PASSWORD") "open-bank"))

(defn datasource-options
  [db-port db-hostname db-password]
  {:auto-commit        true
   :read-only          false
   :connection-timeout 30000
   :validation-timeout 5000
   :idle-timeout       600000
   :max-lifetime       1800000
   :minimum-idle       10
   :maximum-pool-size  10
   :pool-name          "db-pool"
   :adapter            "postgresql"
   :username           "clojure_ge"
   :password           db-password
   :database-name      "transactiondb"
   :server-name        db-hostname
   :port-number        db-port
   :register-mbeans    false})

(defrecord PostgresDatabase []

  component/Lifecycle

  (start [this]
    (let [datasource (h/make-datasource (datasource-options db-port db-hostname db-password))]
      (assoc this :datasource datasource)))

  (stop [this]
    (h/close-datasource (:datasource this))
    (assoc this :datasource nil)))

(defn new-db
  []
  {:db (map->PostgresDatabase {})})
