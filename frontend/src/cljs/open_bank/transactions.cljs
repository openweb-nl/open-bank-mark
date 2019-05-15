(ns open-bank.transactions
  (:require [re-graph.core :as re-graph]))

(def common-params
  "$iban: String!, $show_iban: Boolean!, $show_new_balance: Boolean!,  $show_direction: Boolean!, $show_from_to: Boolean!, $show_changed_by: Boolean!, $show_descr: Boolean!")

(def details
  "{iban @include(if: $show_iban)
  new_balance @include(if: $show_new_balance)
  direction @include(if: $show_direction)
  id
  from_to @include(if: $show_from_to)
  changed_by @include(if: $show_changed_by)
  descr @include(if: $show_descr)}")

(def query-query (str "($max_items: Int!, " common-params "){transactions_by_iban(iban: $iban max_items: $max_items) " details "}"))

(def subscription-query (str "(" common-params "){stream_transactions (iban: $iban) " details "}"))

(defn get-arguments-map
  [db]
  (if-let [iban (case (:selected-nav db)
                  :home (:company-iban db)
                  :bank-employee (:employee-iban db)
                  :client (get-in db [:login-status :iban])
                  nil)]
    (-> (:show-arguments db)
        (assoc :iban iban)
        (assoc :max_items (:max-items db)))))

(defn get-dispatches
  [db]
  (if-let [arguments-map (get-arguments-map db)]
    (let [new-sub-id (inc (:subscription-id db))]
      (if-let [ats (:active-t-subscription db)]
        [[::re-graph/unsubscribe (keyword (str "transactions-" ats))]
         [::re-graph/query query-query arguments-map [:open-bank.events/reset-transactions]]
         [::re-graph/subscribe (keyword (str "transactions-" new-sub-id)) subscription-query arguments-map [:open-bank.events/on-transaction]]
         [:open-bank.events/set-subscription-id new-sub-id]]
        [[::re-graph/query query-query arguments-map [:open-bank.events/reset-transactions]]
         [::re-graph/subscribe (keyword (str "transactions-" new-sub-id)) subscription-query arguments-map [:open-bank.events/on-transaction]]
         [:open-bank.events/set-subscription-id new-sub-id]]))
    (if-let [ats (:active-t-subscription db)]
      [[::re-graph/unsubscribe (keyword (str "transactions-" ats))]
       [:open-bank.events/remove-active-t-subscription]
       [:open-bank.events/remove-transactions]]
      [[:open-bank.events/remove-transactions]])))