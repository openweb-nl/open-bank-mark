(ns nl.openweb.test.load
  (:require [nrepl.core :as repl])
  (:import (java.io IOException)))

(def conn (atom nil))
(def client (atom nil))

(defn close-conn
  []
  (if-let [c @conn]
    (try
      (.close c)
      (catch IOException e
        (println e "Failed to close " @conn)))))

(defn reconnect
  []
  (close-conn)
  (reset! conn (repl/connect :port 17888))
  (reset! client (repl/client @conn 1000))
  (Double/NaN))

(defn safe-read
  [input]
  (if (nil? input) (reconnect) (read-string input)))

(defn set-batch-size
  [batch-size]
  (let [code (str "(do (ns nl.openweb.heartbeat.core) (reset! batch-size " batch-size "))")
        set-batch-size (-> (repl/message @client {:op :eval :code code})
                           first
                           :value
                           safe-read)]
    (if (Double/isNaN set-batch-size)
      (recur batch-size)
      set-batch-size)))

(defn set-sleep-time
  [sleep-time]
  (let [code (str "(do (ns nl.openweb.heartbeat.core) (reset! sleep-time " sleep-time "))")
        set-sleep-time (-> (repl/message @client {:op :eval :code code})
                           first
                           :value
                           safe-read)]
    (if (Double/isNaN set-sleep-time)
      (recur sleep-time)
      set-batch-size)))

(defn init
  []
  (reconnect)
  (set-sleep-time 100)
  (set-batch-size 0))

(defn get-counter
  []
  (let [code (str "(do (ns nl.openweb.heartbeat.core) @counter)")]
    (-> (repl/message @client {:op :eval :code code})
        first
        :value
        safe-read)))

(defn close
  []
  (set-batch-size 0)
  (close-conn)
  (reset! conn nil)
  (reset! client nil))
