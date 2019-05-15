(defproject open-bank "0.1.0-SNAPSHOT"
  :description "front-end for the kafka workshop"
  :url "https://stash.open-web.nl/projects/OP/repos/open-bank/browse"
  :dependencies [[org.clojure/clojure "1.10.0"]
                 [org.clojure/clojurescript "1.10.520"]
                 [reagent "0.8.1"]
                 [re-frame "0.10.6"]
                 [re-graph "0.1.8"]]
  :plugins [[lein-cljsbuild "1.1.5"]
            [lein-sass "0.5.0"]]
  :min-lein-version "2.5.3"
  :source-paths ["src/clj"]
  :clean-targets ^{:protect false} ["resources/public/js/compiled" "target" "resources/public/css"]
  :figwheel {:css-dirs ["resources/public/css"]}
  :sass {:src              "resources/app/stylesheets"
         :output-directory "resources/public/css"
         :source-maps      false
         :command          :sassc}
  :profiles
  {:dev
   {:dependencies [[binaryage/devtools "0.9.10"]
                   [day8.re-frame/re-frame-10x "0.4.0"]]
    :plugins      [[lein-figwheel "0.5.15"]]}}
  :cljsbuild
  {:builds
   [{:id           "dev"
     :source-paths ["src/cljs"]
     :figwheel     {:on-jsload "open-bank.core/mount-root"}
     :compiler     {:main                 open-bank.core
                    :output-to            "resources/public/js/compiled/app.js"
                    :output-dir           "resources/public/js/compiled/out"
                    :optimizations        :none
                    :asset-path           "js/compiled/out"
                    :source-map-timestamp true
                    :preloads             [devtools.preload
                                           day8.re-frame-10x.preload]
                    :closure-defines      {"re_frame.trace.trace_enabled_QMARK_" true}
                    :external-config      {:devtools/config {:features-to-install :all}}}}

    {:id           "min"
     :source-paths ["src/cljs"]
     :compiler     {:main            open-bank.core
                    :output-to       "resources/public/js/compiled/app.js"
                    :optimizations   :advanced
                    :closure-defines {goog.DEBUG false}
                    :pretty-print    false}}]})
