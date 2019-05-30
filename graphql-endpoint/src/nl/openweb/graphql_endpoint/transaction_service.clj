(ns nl.openweb.graphql-endpoint.transaction-service
  (:require [com.stuartsierra.component :as component]
            [clojurewerkz.money.amounts :as ma]
            [clojurewerkz.money.currencies :as cu]
            [clojurewerkz.money.format :as fo]
            [next.jdbc :as j]
            [next.jdbc.sql :as sql]
            [nl.openweb.topology.clients :as clients])
  (:import (nl.openweb.data BalanceChanged)
           (java.util Locale)))

(def bc-topic (or (System/getenv "KAFKA_BC_TOPIC") "balance_changed"))
(def client-id (or (System/getenv "KAFKA_CLIENT_ID") "graphql-endpoint-transactions"))
(def euro-c (cu/for-code "EUR"))
(def dutch-locale (Locale. "nl" "NL"))

(defn bc->sql-transaction
  "id needs to be gotten from db and not class"
  [^BalanceChanged bc]
  (let [^long changed-by (.getChangedBy bc)]
    {:iban        (.getIban bc)
     :new_balance (fo/format (ma/amount-of euro-c (/ (.getNewBalance bc) 100)) dutch-locale)
     :changed_by  (fo/format (ma/amount-of euro-c (/ (Math/abs changed-by) 100)) dutch-locale)
     :from_to     (.getFromTo bc)
     :direction   (if (< changed-by 0) "DEBIT" "CREDIT")
     :descr       (.getDescription bc)}))

(defn sql-transaction->graphql-transaction
  ([sql-map]
   {:id          (:transaction/id sql-map)
    :iban        (:transaction/iban sql-map)
    :new_balance (:transaction/new_balance sql-map)
    :changed_by  (:transaction/changed_by sql-map)
    :from_to     (:transaction/from_to sql-map)
    :direction   (:transaction/direction sql-map)
    :descr       (:transaction/descr sql-map)})
  ([sql-map changed-by-long]
   (assoc (sql-transaction->graphql-transaction sql-map) :cbl changed-by-long)))

(defn insert-transaction!
  [datasource transaction]
  (with-open [conn (j/get-connection datasource)]
    (sql/insert! conn :transaction transaction)))

(defn add-bc
  [cr datasource subscriptions]
  (let [^BalanceChanged bc (.value cr)
        sql-transaction (bc->sql-transaction bc)
        sql-map (insert-transaction! datasource sql-transaction)
        graphql-transaction (sql-transaction->graphql-transaction sql-map (.getChangedBy bc))]
    (doseq [[filter-f source-stream] (vals (:map @subscriptions))]
      (if (filter-f graphql-transaction)
        (source-stream graphql-transaction)))))

(defrecord TransactionService []

  component/Lifecycle

  (start [this]
    (let [subscriptions (atom {:id 0 :map {}})
          stop-consume-f (clients/consume client-id client-id bc-topic #(add-bc % (get-in this [:db :datasource]) subscriptions))]
      (-> this
          (assoc :subscriptions subscriptions)
          (assoc :stop-consume stop-consume-f))))

  (stop [this]
    ((:stop-consume this))
    (doseq [[_ source-stream] (vals (:map @(:subscriptions this)))]
      (source-stream nil))
    (-> this
        (assoc :subscriptions nil)
        (assoc :stop-consume nil))))

(defn new-service
  []
  {:transaction-service (-> {}
                  map->TransactionService
                  (component/using [:db]))})

(defn find-transaction-by-id
  [db id]
  (if-let [sql-transaction (with-open [conn (j/get-connection (get-in db [:db :datasource]))]
                              (j/execute-one! conn ["SELECT * FROM transaction WHERE id = ?" id]))]
    (sql-transaction->graphql-transaction sql-transaction)))

(defn find-transactions-by-iban
  [db iban max-msg]
  (if-let [sql-transactions (with-open [conn (j/get-connection (get-in db [:db :datasource]))]
                              (j/execute! conn ["SELECT * FROM transaction WHERE iban = ? ORDER BY id DESC LIMIT ?" iban max-msg]))]
    (map sql-transaction->graphql-transaction sql-transactions)))

(defn find-all-last-transactions
  [db]
  (if-let [sql-transactions (with-open [conn (j/get-connection (get-in db [:db :datasource]))]
                              (j/execute! conn ["SELECT * FROM transaction WHERE id IN (SELECT MAX(id) FROM transaction GROUP BY iban) ORDER BY iban"]))]
    (map sql-transaction->graphql-transaction sql-transactions)))

(defn add-stream
  [subscriptions filter-f source-stream]
  (let [new-id (inc (:id subscriptions))]
    (-> subscriptions
        (assoc :id new-id)
        (assoc-in [:map new-id] [filter-f source-stream]))))

(defn create-transaction-subscription
  [db source-stream iban min_amount direction]
  (let [filter-f (fn [bc-map]
                   (cond
                     (and iban (not= iban (:iban bc-map))) false
                     (and min_amount (< (:cbl bc-map) min_amount)) false
                     (and direction (not= direction (:direction bc-map))) false
                     :else true))
        subscriptions (swap! (:subscriptions db) add-stream filter-f source-stream)]
    (:id subscriptions)))

(defn stop-transaction-subscription
  [db id]
  (let [source-stream (second (get (:map @(:subscriptions db)) id))]
    (when source-stream
      (source-stream nil)
      (swap! (:subscriptions db) #(update % :map dissoc id)))))