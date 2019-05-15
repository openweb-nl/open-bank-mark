(ns nl.openweb.graphql-endpoint.schema
  "Contains custom resolvers and a function to provide the full schema."
  (:require
    [clojure.java.io :as io]
    [clojure.tools.logging :as log]
    [com.walmartlabs.lacinia.util :as util]
    [com.walmartlabs.lacinia.schema :as schema]
    [com.stuartsierra.component :as component]
    [nl.openweb.graphql-endpoint.account-creation-service :as a-service]
    [nl.openweb.graphql-endpoint.money-transfer-service :as m-service]
    [nl.openweb.graphql-endpoint.transaction-service :as t-service]
    [clojure.edn :as edn]))

(defn transaction-by-id
  [t-service]
  (fn [_ args _]
    (t-service/find-transaction-by-id t-service (:id args))))

(defn transactions-by-iban
  [t-service]
  (fn [_ args _]
    (t-service/find-transactions-by-iban t-service (:iban args) (:max_items args))))

(defn all-last-transactions
  [t-service]
  (fn [_ _ _]
    (t-service/find-all-last-transactions t-service)))

(defn stream-transactions
  [t-service]
  (fn [_ args source-stream]
    (log/debug "starting transaction subscription with args" args)
    ;; Create an object for the subscription.
    (let [id (t-service/create-transaction-subscription t-service source-stream (:iban args) (:min_amount args) (:direction args))]
      ;; Return a function to cleanup the subscription
      #(t-service/stop-transaction-subscription t-service id))))

(defn get-account
  [a-service]
  (fn [_ args source-stream]
    (let [id (a-service/get-account a-service source-stream (:username args) (:password args))]
      #(a-service/stop-transaction-subscription a-service id))))

(defn money-transfer
  [m-service]
  (fn [_ args source-stream]
    (log/debug "starting make money transfer subscription with args:" args)
    (let [id (m-service/money-transfer m-service source-stream args)]
      #(m-service/stop-transaction-subscription m-service id))))

(defn resolver-map
  [component]
  (let [t-service (:t-service component)]
    {:query/transaction-by-id     (transaction-by-id t-service )
     :query/transactions-by-iban  (transactions-by-iban t-service )
     :query/all-last-transactions (all-last-transactions t-service )
     }))

(defn stream-map
  [component]
  (let [t-service (:t-service component)
        a-service (:a-service component)
        m-service (:m-service component)]
    {:stream-transactions (stream-transactions t-service)
     :get-account (get-account a-service)
     :money-transfer (money-transfer m-service)
     }))

(defn load-schema
  [component]
  (-> (io/resource "graphql-schema.edn")
      slurp
      edn/read-string
      (util/attach-resolvers (resolver-map component))
      (util/attach-streamers (stream-map component))
      schema/compile))

(defrecord SchemaProvider [schema]
  component/Lifecycle
  (start [this]
    (assoc this :schema (load-schema this)))
  (stop [this]
    (assoc this :schema nil)))

(defn new-schema-provider
  []
  {:schema-provider (-> {}
                        map->SchemaProvider
                        (component/using [:t-service :a-service :m-service]))})
