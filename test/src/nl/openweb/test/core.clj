(ns nl.openweb.test.core
  (:require [clojure.test :refer :all]
            [nl.openweb.test.analysis :as analysis]
            [nl.openweb.test.load :as load]
            [nl.openweb.test.interactions :as interactions]
            [nl.openweb.test.file :as file]
            [nl.openweb.test.process :as process])
  (:import (java.time Instant))
  (:gen-class))

(def max-interaction-time 5000)
(def min-loop-time 1000)
(def max-loops 4000)
(def max-time-outs 3)
(def batch-cycle 40)
(def loops-for-success 800)

(defn init
  []
  (process/init)
  (load/init)
  (file/init)
  (interactions/prep max-interaction-time))

(defn close
  [loop-number]
  (interactions/close)
  (file/close)
  (load/close)
  (process/close)
  (if (> loop-number loops-for-success)
    (System/exit 0)
    (System/exit 1)))

(defn optionally-increase-batch-size
  [batch-size loop-number]
  (if (= 0 (mod loop-number batch-cycle))
    (let [new-batch-size (inc batch-size)]
      (load/set-batch-size new-batch-size)
      (println "set batch-size to" new-batch-size)
      new-batch-size)
    batch-size))

(defn add-row
  [loop-number current-time interaction-time batch-size time-outs]
  (file/add-row loop-number current-time interaction-time batch-size)
  time-outs)

(defn time-out
  [current-time time-outs]
  (let [new-time-outs (inc time-outs)]
    (println "timeout" new-time-outs "occurred at" (str current-time))
    new-time-outs))

(defn add-row-or-time-out
  [loop-number batch-size time-outs interaction-time current-time]
  (if (> max-interaction-time interaction-time)
    (add-row loop-number current-time interaction-time batch-size time-outs)
    (time-out current-time time-outs)))

(defn analytics-loop
  [start loop-number batch-size time-outs]
  (let [interaction-time (interactions/safe-run loop-number)
        current-time (Instant/now)
        new-time-outs (add-row-or-time-out loop-number batch-size time-outs interaction-time current-time)
        millis-till-next (- (+ start (* min-loop-time loop-number)) (inst-ms current-time))]
    (if (pos? millis-till-next) (Thread/sleep millis-till-next))
    (if
      (and
        (> max-time-outs new-time-outs)
        (> max-loops loop-number))
      (recur start (inc loop-number) (optionally-increase-batch-size batch-size loop-number) new-time-outs)
      loop-number)))

(defn do-tests
  []
  (init)
  (let [loop-number (analytics-loop (inst-ms (Instant/now)) 1 0 0)]
    (close loop-number)))

(defn display-test
  [file-name]
  (let [config (file/get-data file-name)
        data (reduce-kv (fn [i k v] (assoc i k (file/get-data v))) {} (:mapping config))]
    (analysis/process (:category-name config) data)))

(defn -main
  [& [file-name]]
  (if (nil? file-name)
    (do-tests)
    (display-test file-name)))
