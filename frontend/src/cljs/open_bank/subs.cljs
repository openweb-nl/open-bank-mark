(ns open-bank.subs
  (:require [re-frame.core :as re-frame]))

(re-frame/reg-sub
  ::show-left
  (fn [db]
    (:show-left db)))

(re-frame/reg-sub
  ::nav
  (fn [db]
    [(:selected-nav db) (:mob-expand db) (:show-left db)]))

(re-frame/reg-sub
  ::selected-nav
  (fn [db]
    (:selected-nav db)))

(re-frame/reg-sub
  ::left
  (fn [db]
    (case (:selected-nav db)
      :home [(:selected-nav db) (:company-iban db)]
      :bank-employee [(:selected-nav db) [(:all-accounts db) (:employee-iban db)]]
      :client [(:selected-nav db) (:login-status db)]
      :background [(:selected-nav db) nil]
      [:non "error, should not be possible"])))

(re-frame/reg-sub
  ::middle
  (fn [db]
    (case (:selected-nav db)
      :home [(:selected-nav db) (:transactions db)]
      :bank-employee [(:selected-nav db) [(:employee-iban db) (:transactions db)]]
      :client [(:selected-nav db) [(:login-status db) (:transfer-data db) (:transactions db)]]
      :background [(:selected-nav db) nil]
      [:non "error, should not be possible"])))

(re-frame/reg-sub
  ::max-items
  (fn [db]
    (:max-items db)))

(re-frame/reg-sub
  ::show-arguments
  (fn [db]
    (:show-arguments db)))

(re-frame/reg-sub
  ::login-status
  (fn [db]
    (:login-status db)))
