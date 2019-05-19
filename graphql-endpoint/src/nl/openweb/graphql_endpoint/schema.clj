(ns nl.openweb.graphql-endpoint.schema
  "Contains custom resolvers and a function to provide the full schema."
  (:require
    [clojure.java.io :as io]
    [clojure.tools.logging :as log]
    [com.walmartlabs.lacinia.util :as util]
    [com.walmartlabs.lacinia.schema :as schema]
    [com.stuartsierra.component :as component]
    [nl.openweb.graphql-endpoint.account-creation-service :as account-creation-service]
    [nl.openweb.graphql-endpoint.money-transfer-service :as money-transfer-service]
    [nl.openweb.graphql-endpoint.transaction-service :as transaction-service]
    [clojure.edn :as edn]))

(defn transaction-by-id
  [transaction-service]
  (fn [_ args _]
    (transaction-service/find-transaction-by-id transaction-service (:id args))))

(defn transactions-by-iban
  [transaction-service]
  (fn [_ args _]
    (transaction-service/find-transactions-by-iban transaction-service (:iban args) (:max_items args))))

(defn all-last-transactions
  [transaction-service]
  (fn [_ _ _]
    (transaction-service/find-all-last-transactions transaction-service)))

(defn stream-transactions
  [transaction-service]
  (fn [_ args source-stream]
    (log/debug "starting transaction subscription with args" args)
    ;; Create an object for the subscription.
    (let [id (transaction-service/create-transaction-subscription transaction-service source-stream (:iban args) (:min_amount args) (:direction args))]
      ;; Return a function to cleanup the subscription
      #(transaction-service/stop-transaction-subscription transaction-service id))))

(defn get-account
  [account-creation-service]
  (fn [_ args source-stream]
    (let [id (account-creation-service/get-account account-creation-service source-stream (:username args) (:password args))]
      #(account-creation-service/stop-transaction-subscription account-creation-service id))))

(defn money-transfer
  [money-transfer-service]
  (fn [_ args source-stream]
    (log/debug "starting make money transfer subscription with args:" args)
    (let [id (money-transfer-service/money-transfer money-transfer-service source-stream args)]
      #(money-transfer-service/stop-transaction-subscription money-transfer-service id))))

(defn resolver-map
  [component]
  (let [transaction-service (:transaction-service component)]
    {:query/transaction-by-id     (transaction-by-id transaction-service )
     :query/transactions-by-iban  (transactions-by-iban transaction-service )
     :query/all-last-transactions (all-last-transactions transaction-service )
     }))

(defn stream-map
  [component]
  (let [transaction-service (:transaction-service component)
        account-creation-service (:account-creation-service component)
        money-transfer-service (:money-transfer-service component)]
    {:stream-transactions (stream-transactions transaction-service)
     :get-account (get-account account-creation-service)
     :money-transfer (money-transfer money-transfer-service)
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
                        (component/using [:transaction-service :account-creation-service :money-transfer-service]))})
