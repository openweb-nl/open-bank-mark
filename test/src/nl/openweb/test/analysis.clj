(ns nl.openweb.test.analysis
  (:require [kixi.stats.core :as kixi]
            [oz.core :as oz]))

(defn valid
  [row nth-in-row]
  (if (< nth-in-row (count row))
    (let [value (nth row nth-in-row)]
      (not (or (nil? value) (Double/isNaN value))))))

(defn get-statistic
  [coll nth-in-row statistic]
  (->> coll
       (filter #(valid % nth-in-row))
       (transduce (map #(nth % nth-in-row)) statistic)))

(defn vega-item [category [additional-load data-rows]]
  {
   :category        category
   :additional-load additional-load
   :average-latency (get-statistic data-rows 2 kixi/mean)
   :err-latency     (get-statistic data-rows 2 kixi/standard-error)
   :max-latency     (get-statistic data-rows 2 kixi/max)
   :min-latency     (get-statistic data-rows 2 kixi/min)
   :min-count       (get-statistic data-rows 4 kixi/min)
   :average-db-cpu  (get-statistic data-rows 5 kixi/mean)
   :err-db-cpu      (get-statistic data-rows 5 kixi/standard-error)
   :average-db-mem  (get-statistic data-rows 6 kixi/mean)
   :err-db-mem      (get-statistic data-rows 6 kixi/standard-error)
   :average-ch-cpu  (get-statistic data-rows 7 kixi/mean)
   :err-ch-cpu      (get-statistic data-rows 7 kixi/standard-error)
   :average-ch-mem  (get-statistic data-rows 8 kixi/mean)
   :err-ch-mem      (get-statistic data-rows 8 kixi/standard-error)
   :average-kb-cpu  (get-statistic data-rows 9 kixi/mean)
   :err-kb-cpu      (get-statistic data-rows 9 kixi/standard-error)
   :average-kb-mem  (get-statistic data-rows 10 kixi/mean)
   :err-kb-mem      (get-statistic data-rows 10 kixi/standard-error)
   :average-ge-cpu  (get-statistic data-rows 11 kixi/mean)
   :err-ge-cpu      (get-statistic data-rows 11 kixi/standard-error)
   :average-ge-mem  (get-statistic data-rows 12 kixi/mean)
   :err-ge-mem      (get-statistic data-rows 12 kixi/standard-error)
   :data-points     (count data-rows)
   })

(defn group-by-load
  [data-rows]
  (group-by (fn [row] (nth row 3)) data-rows))

(defn group
  [data-rows]
  (if (vector? data-rows)
    (group-by-load data-rows)
    (apply merge-with into (map group-by-load data-rows))))

(defn data-rows->vega [language data-rows]
  (map (partial vega-item language) (group data-rows)))

(defn raw->vega
  [data]
  (reduce-kv (fn [i language data-rows] (into i (data-rows->vega language data-rows))) [] data))

(defn line-plot
  ([vega-items category-name y-value y-title]
   {:width  500,
    :height 500,
    :data   {:values vega-items}
    :layer  [{:encoding {:x     {:field "additional-load"
                                 :type  "quantitative"
                                 :title "Additional load (messages/s)"}
                         :y     {:field y-value
                                 :type  "quantitative"
                                 :title y-title}
                         :color {:field "category"
                                 :type  "nominal"
                                 :title category-name}}
              :mark     {:type  "line"
                         :point {:tooltip {:content "data"}}}
              }]})
  ([vega-items category-name y-value y-title y-err]
   (update (line-plot vega-items category-name y-value y-title) :layer conj
           {
            :encoding {:x      {:field "additional-load"
                                :type  "quantitative"
                                :title "Additional load (messages/s)"}
                       :y      {:field y-value
                                :type  "quantitative"
                                :title y-title}
                       :yError {:field y-err}
                       :color  {:field "category"
                                :type  "nominal"
                                :title category-name}}
            :mark     {:type "errorbar"}
            })))

(defn to-html
  [vega-items category-name y-value y-title]
  (let [y-err (clojure.string/replace y-value #"average" "err")
        lp (if (= y-err y-value)
             (line-plot vega-items category-name y-value y-title)
             (line-plot vega-items category-name y-value y-title y-err))]
    (oz/export! lp (str "frontend/public/" y-value ".html"))))

(def outputs {"average-latency" "Average latency (ms)"
              "max-latency"     "Max latency (ms)"
              "min-latency"     "Min latency (ms)"
              "min-count"       "Min count (heartbeats send)"
              "average-db-cpu"  "Average cpu database (% from total)"
              "average-db-mem"  "Average mem database (MiB)"
              "average-ch-cpu"  "Average cpu command-handler (% from total)"
              "average-ch-mem"  "Average mem command-handler (MiB)"
              "average-kb-cpu"  "Average cpu kafka broker (% from total)"
              "average-kb-mem"  "Average mem kafka broker (MiB)"
              "average-ge-cpu"  "Average cpu graphql endpoint (% from total)"
              "average-ge-mem"  "Average mem graphql endpoint (MiB)"
              "data-points"     "Amount of measurements"})

(defn process
  [category-name data]
  (let [vega-items (raw->vega data)]
    (doseq [[k v] outputs] (to-html vega-items category-name k v))))
