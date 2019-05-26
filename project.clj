(defproject nl.openweb/open-bank-mark "0.1.0-SNAPSHOT"
  :plugins [[lein-modules "0.3.11"]]
  :modules {:inherited
                      {:repositories  [["confluent" "https://packages.confluent.io/maven/"]]
                       :aliases       {"all" ^:displace ["do" "clean," "test," "install", "uberjar"]
                                       "-f"  ["with-profile" "+fast"]}
                       :scm           {:dir ".."}
                       :javac-options ["-target" "11" "-source" "11"]
                       :license       {:name "MIT License"
                                       :url  "https://opensource.org/licenses/MIT"
                                       :key  "mit"
                                       :year 2019}}
            :versions {ch.qos.logback/logback-classic                 "1.2.3"
                       com.damballa/abracad                           "0.4.13"
                       com.fasterxml.jackson.core/jackson-annotations "2.9.8"
                       com.fasterxml.jackson.core/jackson-core        "2.9.8"
                       com.fasterxml.jackson.core/jackson-databind    "2.9.8"
                       hikari-cp/hikari-cp                            "2.7.1"
                       io.confluent/kafka-avro-serializer             "5.2.1"
                       org.apache.avro/avro                           "1.8.2"
                       org.clojure/clojure                            "1.10.0"
                       org.clojure/data.json                          "0.2.6"
                       org.clojure/tools.logging                      "0.4.1"
                       org.postgresql/postgresql                      "42.2.5"
                       nrepl/nrepl                                    "0.6.0"
                       seancorfield/next.jdbc                         "1.0.0-beta1"}})
