(ns open-bank.core
  (:require [reagent.core :as reagent]
            [re-frame.core :as re-frame]
            [re-graph.core :as re-graph]
            [open-bank.config :as config]
            [open-bank.db :refer [default-db]]
            [open-bank.transactions :refer [get-dispatches]]
            [open-bank.events :as events]
            [open-bank.views :as views]))

(defn dev-setup []
  (when config/debug?
    (enable-console-print!)
    (println "dev mode")))

(defn mount-root []
  (re-frame/clear-subscription-cache!)
  (reagent/render [views/main-panel]
                  (.getElementById js/document "app")))

(defn ^:export init []
  (re-frame/dispatch-sync [::events/initialize-db])
  (re-frame/dispatch [::re-graph/init {:ws-url   "ws://localhost:8888/graphql-ws"
                                       :http-url "http://localhost:8888/graphql"}])
  (doseq [dispatch (get-dispatches default-db)]
    (re-frame/dispatch dispatch))
  (dev-setup)
  (mount-root))