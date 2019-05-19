(ns nl.openweb.graphql-endpoint.viz
  (:require [nl.openweb.graphql-endpoint.system :as system]
            [com.walmartlabs.system-viz :refer [visualize-system]]))

(defn -main
  []
  (visualize-system (system/new-system) {:format  :svg})
  (System/exit 0))
