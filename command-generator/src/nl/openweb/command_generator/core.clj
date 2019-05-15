(ns nl.openweb.command-generator.core
  (:require [clj-time.local :as time-local]
            [nl.openweb.topology.clients :as clients]
            [nl.openweb.topology.value-generator :as vg])
  (:import (java.util UUID)
           (org.apache.kafka.clients.consumer ConsumerRecord)
           (nl.openweb.data AccountCreationConfirmed Uuid Atype ConfirmMoneyTransfer ConfirmAccountCreation))
  (:gen-class))

(def heartbeat-topic (or (System/getenv "KAFKA_HEARTBEAT_TOPIC") "heartbeat"))
(def cac-topic (or (System/getenv "KAFKA_CAC_TOPIC") "confirm_account_creation"))
(def acc-topic (or (System/getenv "KAFKA_ACC_TOPIC") "account_creation_confirmed"))
(def cmt-topic (or (System/getenv "KAFKA_CMT_TOPIC") "confirm_money_transfer"))
(def client-id (or (System/getenv "KAFKA_CLIENT_ID") "command-generator"))

(def company-iban {:iban "NL66OPEN0000000000" :token "00000000000000000000"})
(def ah "Albert Heijn BV")
(def cash-amounts (mapv #(* 1000 %) [2 2 2 3 5 5 10 20]))
(def accounts (atom []))

(defn get-iban
  []
  (if (not-empty @accounts)
    (rand-nth @accounts)
    company-iban))

(defn get-type
  []
  (let [rand (rand-int 70)]
    (cond
      (zero? rand) :salary
      (< rand 4) :deposit
      (< rand 14) :withdraw
      (< rand 54) :ah-pin
      :else :something-else)))

(def description-map
  {:salary         "salary"
   :deposit        "deposit"
   :withdraw       "withdraw"
   :ah-pin         "ah pin"
   :something-else "transfer"})

(defn get-description
  [rand-helper]
  (str (rand-helper description-map) " " (time-local/to-local-date-time (time-local/local-now))))

(defn create-transfer-map
  ([]
   (let [some-type (get-type)
         to (cond (= some-type :withdraw) "cash"
                  (= some-type :ah-pin) ah
                  :else (:iban (get-iban)))]
     (create-transfer-map some-type to)))
  ([some-type to]
   (let [from-iban (if (= some-type :salary) company-iban (get-iban))]
     (-> {}
         (assoc :id (vg/uuid->bytes (UUID/randomUUID)))
         (assoc :token (if (= some-type :deposit) "cash" (:token from-iban)))
         (assoc :amount (cond
                          (= some-type :salary) (+ 220000 (rand-int 50000))
                          (or (= some-type :deposit) (= some-type :withdraw)) (rand-nth cash-amounts)
                          :else (+ 1000 (rand-int 10000))))
         (assoc :from (if (= some-type :deposit) "cash" (:iban from-iban)))
         (assoc :to to)
         (assoc :description (get-description some-type))))))

(defn get-cmt
  [t]
  (ConfirmMoneyTransfer. (Uuid. (:id t)) (:token t) (:amount t) (:from t) (:to t) (:description t)))

(defn add-account
  [producer ^ConsumerRecord consumer-record]
  (let [^AccountCreationConfirmed value (.value consumer-record)]
    (when (= Atype/AUTO (.getAType value))
      (swap! accounts conj (-> {}
                               (assoc :token (.getToken value))
                               (assoc :iban (.getIban value))))
      (clients/produce producer cmt-topic (get-cmt (create-transfer-map :salary (.getIban value)))))))

(defn generate-command
  [producer consumer-record]
  (if-let [beat (.getBeat (.value consumer-record))]
    (if
      (or (< beat 20) (zero? (mod beat 200)))
      (clients/produce producer cac-topic (ConfirmAccountCreation. (Uuid. (vg/uuid->bytes (UUID/randomUUID))) Atype/AUTO))
      (clients/produce producer cmt-topic (get-cmt (create-transfer-map))))))

(defn -main
  []
  (let [producer (clients/get-producer client-id)]
    (clients/consume (str client-id "-hb") client-id heartbeat-topic (partial generate-command producer))
    (clients/consume (str client-id "-acc") client-id acc-topic (partial add-account producer))))