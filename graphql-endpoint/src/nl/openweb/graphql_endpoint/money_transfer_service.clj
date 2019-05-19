(ns nl.openweb.graphql-endpoint.money-transfer-service
  (:require [com.stuartsierra.component :as component]
            [nl.openweb.topology.clients :as clients]
            [nl.openweb.topology.value-generator :refer [bytes->uuid uuid->bytes]]
            [clojure.tools.logging :as log])
  (:import (org.apache.kafka.clients.consumer ConsumerRecord)
           (nl.openweb.data Uuid MoneyTransferConfirmed MoneyTransferFailed MoneyTransferConfirmed ConfirmMoneyTransfer)
           (java.util UUID)))

(def cmt-topic (or (System/getenv "KAFKA_CMT_TOPIC") "confirm_money_transfer"))
(def mtc-topic (or (System/getenv "KAFKA_MTC_TOPIC") "money_transfer_confirmed"))
(def mtf-topic (or (System/getenv "KAFKA_MTF_TOPIC") "money_transfer_failed"))
(def client-id (or (System/getenv "KAFKA_CLIENT_ID") "graphql-endpoint-money-transfer"))

(defn v->map
  [v]
  (cond
    (instance? MoneyTransferConfirmed v)
    {:uuid    (str (bytes->uuid (.bytes (.getId v))))
     :success true
     :reason  nil}
    (instance? MoneyTransferFailed v)
    {:uuid    (str (bytes->uuid (.bytes (.getId v))))
     :success false
     :reason  (.getReason v)}))

(defn handle-reply
  [^ConsumerRecord cr transfer-requests subscriptions]
  (let [v-map (v->map (.value cr))]
    (when (get @transfer-requests (:uuid v-map))
      (swap! transfer-requests assoc (:uuid v-map) v-map)
      (doseq [[s-id source-stream] (vals (:map @subscriptions))]
        (if (= s-id (:uuid v-map))
          (source-stream v-map))))))

(defn add-stream
  [subscriptions id source-stream]
  (let [new-id (inc (:id subscriptions))]
    (-> subscriptions
        (assoc :id new-id)
        (assoc-in [:map new-id] [id source-stream]))))

(defn add-sub
  [subscriptions id source-stream]
  (let [new-subscriptions (swap! subscriptions add-stream id source-stream)]
    (:id new-subscriptions)))

(defn create-money-transfer
  [^UUID uuid args]
  (let [id (Uuid. (uuid->bytes uuid))]
    (ConfirmMoneyTransfer.
      id
      (:token args)
      (long (:amount args))
      (:from args)
      (:to args)
      (:descr args))))

(defn money-transfer
  [db source-stream args]
  (let [uuid (:uuid args)]
    (try
      (UUID/fromString uuid)
      (if-let [past-request (get @(:transfer-requests db) uuid)]
        (if (empty? past-request)
          (add-sub (:subscriptions db) uuid source-stream)
          (source-stream past-request))
        (let [sub-id (add-sub (:subscriptions db) uuid source-stream)]
          (swap! (:transfer-requests db) assoc uuid {})
          (clients/produce (get-in db [:kafka-producer :producer]) cmt-topic (create-money-transfer (UUID/fromString uuid) args))
          sub-id))
      (catch IllegalArgumentException e (log/warn uuid "is not valid" e)))))

(defrecord MoneyTransferService []

  component/Lifecycle

  (start [this]
    (let [subscriptions (atom {:id 0 :map {}})
          transfer-requests (atom {})
          stop-consume-f (clients/consume client-id client-id [mtc-topic mtf-topic] #(handle-reply % transfer-requests subscriptions))]
      (-> this
          (assoc :subscriptions subscriptions)
          (assoc :transfer-requests transfer-requests)
          (assoc :stop-consume stop-consume-f))))

  (stop [this]
    ((:stop-consume this))
    (doseq [[_ source-stream] (vals (:map @(:subscriptions this)))]
      (source-stream nil))
    (-> this
        (assoc :subscriptions nil)
        (assoc :transfer-requests nil)
        (assoc :stop-consume nil))))

(defn new-service
  []
  {:money-transfer-service (-> {}
             map->MoneyTransferService
             (component/using [:kafka-producer]))})

(defn stop-transaction-subscription
  [db id]
  (let [source-stream (second (get (:map @(:subscriptions db)) id))]
    (when source-stream
      (source-stream nil)
      (swap! (:subscriptions db) #(update % :map dissoc id)))))