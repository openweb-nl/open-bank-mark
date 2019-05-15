(ns nl.openweb.command-handler.db
  (:require
    [hikari-cp.core :as h]
    [nl.openweb.topology.value-generator :as vg]
    [next.jdbc :as j]
    [next.jdbc.sql :as sql])
  (:import (nl.openweb.data ConfirmMoneyTransfer)
           (java.util UUID)))

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
   :username           "clojure_ch"
   :password           db-password
   :database-name      "balancedb"
   :server-name        db-hostname
   :port-number        db-port
   :register-mbeans    false})

(defonce datasource (atom nil))

(defn init
  []
  (reset! datasource (h/make-datasource (datasource-options db-port db-hostname db-password))))

(defn deref-first
  [refs]
  (if-let [f (first refs)]
    (do
      @f
      (rest refs))))

(defn find-balance-by-iban
  [iban]
  (if
    (vg/valid-open-iban iban)
    (with-open [conn (j/get-connection @datasource)]
      (j/execute-one! conn ["SELECT * FROM balance WHERE iban = ?" iban]))))

(defn insert-balance!
  [mp]
  (let [cmp (-> mp
                (dissoc :uuid)
                (dissoc :reason))]
    (with-open [conn (j/get-connection @datasource)]
      (sql/insert! conn :balance cmp))))

(defn update-balance!
  [mp]
  (with-open [conn (j/get-connection @datasource)]
    (sql/update! conn :balance mp ["balance_id = ?" (:balance/balance_id mp)])))

(defn find-cac-by-uuid
  [uuid]
  (with-open [conn (j/get-connection @datasource)]
    (j/execute-one! conn ["SELECT * FROM cac WHERE uuid = ?" uuid])))

(defn insert-cac!
  [mp]
  (with-open [conn (j/get-connection @datasource)]
    (sql/insert! conn :cac mp)))

(defn find-cmt-by-uuid
  [uuid]
  (with-open [conn (j/get-connection @datasource)]
    (j/execute-one! conn ["SELECT * FROM cmt WHERE uuid = ?" uuid])))

(defn insert-cmt!
  [mp]
  (with-open [conn (j/get-connection @datasource)]
    (sql/insert! conn :cmt mp)))

(defn get-account
  [uuid type]
  (if-let [result (find-cac-by-uuid uuid)]
    {:uuid   (:cac/uuid result)
     :iban   (:cac/iban result)
     :token  (:cac/token result)
     :type   (:cac/type result)
     :reason (:cac/reason result)}
    (let [iban (vg/new-iban)
          reason (if (find-balance-by-iban iban) "generated iban already exists, try again")
          mp {:uuid uuid :iban iban :token (vg/new-token) :type type :reason reason}]
      (insert-cac! mp)
      (if (nil? (:reason mp)) (insert-balance! mp))
      mp)))

(defn transfer-update!
  [uuid ^ConfirmMoneyTransfer tm]
  (if-let [from-map (find-balance-by-iban (.getFrom tm))]
    (cond
      (not (= (:balance/token from-map) (.getToken tm))) {:uuid uuid :reason "invalid token"}
      (< (- (:balance/amount from-map) (.getAmount tm)) (:balance/lmt from-map)) {:uuid uuid :reason "insufficient funds"}
      :else (let [new-from-map (update from-map :balance/amount #(- % (.getAmount tm)))
                  to-map (find-balance-by-iban (.getTo tm))
                  new-to-map (if to-map (update to-map :balance/amount #(+ % (.getAmount tm))))]
              (update-balance! new-from-map)
              (if new-to-map (update-balance! new-to-map))
              (if new-to-map
                {:from new-from-map :to new-to-map}
                {:from new-from-map})))
    (if-let [to-map (find-balance-by-iban (.getTo tm))]
      (let [new-to-map (update to-map :balance/amount #(+ % (.getAmount tm)))]
        (update-balance! new-to-map)
        {:to new-to-map})
      {:uuid uuid :reason "both to and from not known at this bank"})))

(defn invalid-from
  [from]
  (if (= from "cash")
    false
    (not (vg/valid-open-iban from))))

(defn do-transfer!
  [uuid ^ConfirmMoneyTransfer tm]
  (let [result (cond
                 (invalid-from (.getFrom tm)) {:uuid uuid :reason (str "from is invalid")}
                 (= (.getFrom tm) (.getTo tm)) {:uuid uuid :reason "from and to can't be same for transfer"}
                 :else (transfer-update! uuid tm))]
    (if
      (:reason result)
      (insert-cmt! result)
      (insert-cmt! {:uuid uuid}))
    result))

(defn transfer
  [^ConfirmMoneyTransfer tm]
  (let [uuid (-> (.getId tm)
                 .bytes
                 vg/bytes->uuid)]
    (if-let [result (find-cmt-by-uuid uuid)]
      {:uuid   (:cmt/uuid result)
       :reason (:cmt/reason result)}
      (do-transfer! uuid tm))))