(ns nl.openweb.synchronizer.core
  (:require [nl.openweb.synchronizer.admin :as admin]
            [nl.openweb.synchronizer.schema :as schema]
            [nl.openweb.topology.core :as topology])
  (:gen-class))

(defn exists-reduce
  [set [to-create to-check] k v]
  (if (contains? set k)
    [to-create (assoc to-check k v)]
    [(assoc to-create k v) to-check]))

(defn synchronize
  []
  (admin/start)
  (doseq [topic (keys topology/topology)]
    (schema/set-schema topic))
  (let [nodes (admin/describe-cluster-nodes)
        current-topics (admin/list-topics)
        [to-create to-check] (reduce-kv (partial exists-reduce current-topics) [{} {}] topology/topology)
        to-delete (reduce #(remove (fn [a] (= a (first %2))) %1) current-topics to-check)]
    (admin/create-topics to-create (count nodes))
    (admin/delete-topics to-delete)))

(defn -main
  []
  (synchronize)
  (System/exit 0))