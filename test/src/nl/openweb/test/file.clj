(ns nl.openweb.test.file
  (:require [clojure.edn :as edn]
            [nl.openweb.test.load :as load]
            [nl.openweb.test.process :as process]
            [clojure.string :as string])
  (:import (java.util Date)
           (java.text SimpleDateFormat)))

(defonce file-name (atom nil))
(defonce rows (atom []))
(defonce writer (atom nil))
(defonce keep-writing (atom true))
(def base-file-name "clojure-")

(defn get-path
  ([]
   (get-path (str base-file-name (.format (SimpleDateFormat. "yyyy-MM-dd-HH-mm") (Date.)))))
  ([name]
   (str "resources/" name ".edn")))

(defn add-row
  [loop-number current-time interaction-time batch-size]
  (swap! rows conj [loop-number current-time interaction-time batch-size]))

(defn add
  [line]
  (spit @file-name (str line "\n") :append true))

(defn write-row
  [[loop-number current-time interaction-time batch-size]]
  (if (= 0 (mod loop-number 20))
    (add (into [loop-number (inst-ms current-time) interaction-time (* 10 batch-size) (load/get-counter)] (process/get-info)))
    (add [loop-number (inst-ms current-time) interaction-time (* 10 batch-size)])))

(defn write
  []
  (when @keep-writing
    (let [[list _] (reset-vals! rows [])]
      (if
        (empty? list)
        (Thread/sleep 1000)
        (doseq [row list]
          (write-row row))))
    (recur)))

(defn init
  []
  (reset! file-name (get-path))
  (println "creating file" @file-name "for the data")
  (clojure.java.io/make-parents @file-name)
  (spit @file-name "[\n")
  (reset! writer (future (write))))

(defn close
  []
  (reset! keep-writing false)
  (when-let [future @writer]
    @future
    (reset! writer nil))
  (spit @file-name "]" :append true)
  (println (edn/read-string (slurp @file-name)))
  (reset! file-name nil))

(defn read-and-combine-matching
  [name]
  (let [files (file-seq (clojure.java.io/file "resources"))]
    (->> files
         (filter (fn [file] (string/starts-with? (.getName file) name)))
         (map (fn [file] (edn/read-string (slurp file)))))))

(defn get-data
  [name]
  (if
    (string/ends-with? name "*")
    (read-and-combine-matching (subs name 0 (- (count name) 1)))
    (edn/read-string (slurp (get-path name)))))
