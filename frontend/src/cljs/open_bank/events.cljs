(ns open-bank.events
  (:require [re-frame.core :as re-frame]
            [re-graph.core :as re-graph]
            [open-bank.transactions :refer [get-dispatches]]
            [open-bank.db :as db]))

(re-frame/reg-event-db
  ::initialize-db
  (fn [_ _]
    db/default-db))

(re-frame/reg-event-db
  ::on-transaction
  (fn [db [_ {:keys [data errors] :as payload}]]
    (assoc db :last-transaction data)))

(re-frame/reg-event-db
  ::set-all-accounts
  (fn [db [_ {:keys [data errors] :as payload}]]
    (assoc db :all-accounts {:accounts (:all_last_transactions data) :page 1})))

(re-frame/reg-event-db
  ::set-all-accounts-page
  (fn [db [_ page]]
    (assoc-in db [:all-accounts :page] page)))

(re-frame/reg-event-fx
  ::set-selected-nav
  (fn [cofx [_ selected-nav]]
    (let [new-db (assoc (:db cofx) :selected-nav selected-nav)
          dispatches (get-dispatches new-db)]
      {:db         new-db
       :dispatch-n (if (= selected-nav :bank-employee)
                     (conj dispatches [::re-graph/query "{all_last_transactions {iban}}" nil [::set-all-accounts]])
                     dispatches)})))

(re-frame/reg-event-db
  ::toggle-mob-expand
  (fn [db _]
    (update db :mob-expand not)))

(re-frame/reg-event-db
  ::toggle-show-left
  (fn [db _]
    (update db :show-left not)))

(re-frame/reg-event-db
  ::set-subscription-id
  (fn [db [_ subscription-id]]
    (-> db
        (assoc :subscription-id subscription-id)
        (assoc :active-t-subscription subscription-id))))

(re-frame/reg-event-db
  ::remove-active-t-subscription
  (fn [db _]
    (assoc db :active-t-subscription nil)))

(re-frame/reg-event-fx
  ::set-employee-iban
  (fn [cofx [_ employee-iban]]
    (let [new-db (assoc (:db cofx) :employee-iban employee-iban)]
      {:db         new-db
       :dispatch-n (get-dispatches new-db)})))

(re-frame/reg-event-db
  ::on-transaction
  (fn [db [_ {:keys [data errors] :as payload}]]
    (let [old-list (:list (:transactions db))
          new-list (take (:max-items db) (conj old-list (:stream_transactions data)))
          new-last (if (= (:max-items db) (count old-list)) (last old-list))]
      (assoc db :transactions {:list new-list :last new-last}))))

(re-frame/reg-event-db
  ::reset-transactions
  (fn [db [_ {:keys [data errors] :as payload}]]
    (assoc db :transactions {:list (apply list (:transactions_by_iban data)) :last nil})))

(re-frame/reg-event-db
  ::remove-transactions
  (fn [db _]
    (assoc db :transactions nil)))

(re-frame/reg-event-fx
  ::set-max-items
  (fn [cofx [_ m-i]]
    (let [new-db (assoc (:db cofx) :max-items m-i)]
      {:db         new-db
       :dispatch-n (get-dispatches new-db)})))

(re-frame/reg-event-fx
  ::toggle-show
  (fn [cofx [_ t-k]]
    (let [new-db (update-in (:db cofx) [:show-arguments t-k] not)]
      {:db         new-db
       :dispatch-n (get-dispatches new-db)})))

(re-frame/reg-event-fx
  ::logout
  (fn [cofx [_ _]]
    (let [new-db (assoc (:db cofx) :login-status {:valid false})]
      {:db         new-db
       :dispatch-n (get-dispatches new-db)})))

(re-frame/reg-event-fx
  ::get-account
  (fn [cofx [_ username password]]
    {:db       (assoc-in (:db cofx) [:login-status :username] username)
     :dispatch [::re-graph/subscribe
                :get-account
                "($username: String! $password: String!){get_account(username: $username password: $password) {reason iban token}}"
                {:username username :password password}
                [::on-get-account]]}))

(re-frame/reg-event-fx
  ::on-get-account
  (fn [cofx [_ {:keys [data errors] :as payload}]]
    (let [new-db (update (:db cofx) :login-status #(merge % (:get_account data)))]
      {:db         new-db
       :dispatch-n (conj (get-dispatches new-db) [::re-graph/unsubscribe :get-account])})))

(re-frame/reg-event-fx
  ::on-deposit
  (fn [cofx [_ {:keys [data errors] :as payload}]]
    {:db       (assoc (:db cofx) :deposit-data (:money_transfer data))
     :dispatch [::re-graph/unsubscribe (keyword (str "deposit-" (:uuid (:money_transfer data))))]}))

(re-frame/reg-event-fx
  ::on-transfer
  (fn [cofx [_ {:keys [data errors] :as payload}]]
    {:db       (update (:db cofx) :transfer-data #(merge % (:money_transfer data)))
     :dispatch [::re-graph/unsubscribe (keyword (str "transfer-" (:uuid (:money_transfer data))))]}))

(re-frame/reg-event-db
  ::check-valid-login-form
  (fn [db [_ username password]]
    (let [old-valid (get-in db [:login-status :valid])
          new-valid (and (> (count username) 7) (> (count password) 7))]
      (if
        (= old-valid new-valid)
        db
        (assoc db :login-status {:username username
                                 :password password
                                 :valid    new-valid})))))

(re-frame/reg-event-db
  ::check-valid-transfer-form
  (fn [db [_ amount to descr]]
    (let [old-valid (get-in db [:transfer-data :valid])
          int-amount (js/parseInt amount)
          new-valid (and (pos? int-amount) (< int-amount 2147483648) (> (count to) 7) (pos? (count descr)))]
      (if
        (= old-valid new-valid)
        db
        (assoc db :transfer-data {:amount amount
                                  :to     to
                                  :descr  descr
                                  :valid  new-valid})))))