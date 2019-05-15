(ns nl.openweb.test.interactions
  (:require [clojure.stacktrace :refer [print-stack-trace]]
            [clojure.test :refer :all]
            [etaoin.api :refer :all]
            [etaoin.keys :as k])
  (:import (java.time Instant)))

(defonce driver (atom nil))
(def deposit-keys [:deposit-1000 :deposit-2000 :deposit-5000 :deposit-10000 :deposit-20000])
(def deposit-values ["€10,00" "€20,00" "€50,00" "€100,00" "€200,00"])
(def transaction-keys [1111 2323 3489 9999 21078])
(def transaction-descriptions ["foo" "bar" "baz" "present" "fine"])
(def transaction-values ["€11,11" "€23,23" "€34,89" "€99,99" "€210,78"])

(defn wait-till-value
  [expected-value]
  (let [start (inst-ms (Instant/now))]
    (wait-has-text @driver {:css "#transactions div:nth-child(1) div div:nth-child(1) p:nth-child(1) span:nth-child(2)"} expected-value {:interval 0.05 :timeout 5})
    (- (inst-ms (Instant/now)) start)))

(defn run-deposit
  [m]
  (click @driver (nth deposit-keys m))
  (nth deposit-values m))

(defn run-transaction
  [m]
  (let [k (nth transaction-keys m)
        d (nth transaction-descriptions m)]
    (doto @driver
      (fill {:css "#transfer-form div:nth-child(1) input"} k k/enter)
      (fill {:css "#transfer-form div:nth-child(2) input"} "NL66OPEN0000000000" k/enter)
      (fill {:css "#transfer-form div:nth-child(3) input"} d k/enter)
      (click {:css "#transfer-form div:nth-child(4) div a"}))
    (nth transaction-values m)))

(defn run
  [loop-number]
  (let [m (mod loop-number 10)
        v (if (< m 5)
            (run-deposit m)
            (run-transaction (- m 5)))]
    (wait-till-value v)))

(defn safe-run
  [loop-number]
  (try
    (run loop-number)
    (catch Exception error
      (println "An error occurred" (.getMessage error))
      (println "Cause" (.getClass error))
      (print-stack-trace error)
      5000)))

(defn wait-till-button
  []
  (let [start (inst-ms (Instant/now))]
    (wait-exists @driver :deposit-1000 {:interval 0.2 :timeout 30})
    (println "waited for" (- (inst-ms (Instant/now)) start) "ms to log in and see the deposit 10 button")))

(defn login
  []
  (doto @driver
    (set-window-size 1920 1080)
    (go "http://localhost:8181/")
    (wait 2)
    (click {:css "#flex-main-menu a:nth-child(2)"})
    (wait 2)
    (fill {:css "#login-form div:nth-child(1) input"} "testuser" k/enter)
    (fill {:css "#login-form div:nth-child(2) input"} "password" k/enter)
    (wait 1)
    (click {:css "#login-form div:nth-child(3)"})))

  (defn prep
    []
    (reset! driver (chrome-headless))
    (login)
    (wait-till-button))

  (defn close
    []
    (delete-session @driver))
