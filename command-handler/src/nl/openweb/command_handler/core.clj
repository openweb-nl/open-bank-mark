(ns nl.openweb.command-handler.core
  (:require [nl.openweb.command-handler.db :as db]
            [nl.openweb.topology.clients :as clients]
            [nl.openweb.topology.value-generator :as vg])
  (:import (nl.openweb.data ConfirmAccountCreation AccountCreationConfirmed AccountCreationFailed MoneyTransferFailed ConfirmMoneyTransfer MoneyTransferConfirmed BalanceChanged))
  (:gen-class))

(def cac-topic (or (System/getenv "KAFKA_CAC_TOPIC") "confirm_account_creation"))
(def acc-topic (or (System/getenv "KAFKA_ACC_TOPIC") "account_creation_confirmed"))
(def acf-topic (or (System/getenv "KAFKA_ACF_TOPIC") "account_creation_failed"))

(def cmt-topic (or (System/getenv "KAFKA_CMT_TOPIC") "confirm_money_transfer"))
(def mtc-topic (or (System/getenv "KAFKA_MTC_TOPIC") "money_transfer_confirmed"))
(def mtf-topic (or (System/getenv "KAFKA_MTF_TOPIC") "money_transfer_failed"))

(def bc-topic (or (System/getenv "KAFKA_BC_TOPIC") "balance_changed"))

(def client-id (or (System/getenv "KAFKA_CLIENT_ID") "command-handler"))

(defn handle-cac
  [producer]
  (fn [consumer-record]
    (let [cac (.value consumer-record)
          result (db/get-account (-> (.getId cac)
                                     .bytes
                                     vg/bytes->uuid) (.toString (.getAType cac)))]
      (if (:reason result)
        (clients/produce producer acf-topic (AccountCreationFailed. (.getId cac) (:reason result)))
        (clients/produce producer acc-topic (AccountCreationConfirmed. (.getId cac) (:iban result) (:token result) (.getAType cac)))))))

(defn ->bc
  [from ^ConfirmMoneyTransfer cmt balance-row]
  (BalanceChanged.
    (:balance/iban balance-row)
    (:balance/amount balance-row)
    (if from (- (.getAmount cmt)) (.getAmount cmt))
    (if from (.getTo cmt) (.getFrom cmt))
    (.getDescription cmt)))

(defn handle-cmt
  [producer]
  (fn [consumer-record]
    (let [cmt (.value consumer-record)
          result (db/transfer cmt)]
      (if-let [from (:from result)]
        (clients/produce producer bc-topic (->bc true cmt from)))
      (if-let [to (:to result)]
        (clients/produce producer bc-topic (->bc false cmt to)))
      (if-let [reason (:reason result)]
        (clients/produce producer mtf-topic (MoneyTransferFailed. (.getId cmt) reason))
        (clients/produce producer mtc-topic (MoneyTransferConfirmed. (.getId cmt)))
        ))))

(defn -main
  []
  (db/init)
  (let [producer (clients/get-producer client-id)]
    (clients/consume (str client-id "-cac") client-id cac-topic (handle-cac producer))
    (clients/consume (str client-id "-cmt") client-id cmt-topic (handle-cmt producer))))

