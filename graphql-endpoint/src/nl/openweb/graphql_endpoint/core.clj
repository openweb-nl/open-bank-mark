(ns nl.openweb.graphql-endpoint.core
    (:require [nl.openweb.graphql-endpoint.system :as system]
      [com.stuartsierra.component :as component])
    (:gen-class))

(defn -main
      []
      (component/start (system/new-system)))
