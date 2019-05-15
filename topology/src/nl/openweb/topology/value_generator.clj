(ns nl.openweb.topology.value-generator
  (:import (java.util UUID)
           (java.nio ByteBuffer))
  (:gen-class))

(defn new-iban
  ([]
   (new-iban (apply str "0" (repeatedly 9 #(rand-int 10)))))
  ([digits]
   (let [check-nr (- 98 (mod (bigint (str "24251423" digits "232100")) 97))
         check-nr-s (if (< check-nr 10) (str "0" check-nr) (str check-nr))]
     (str "NL" check-nr-s "OPEN" digits))))

(defn valid-open-iban
  [iban]
  (if (= (count iban) 18)
    (= iban (new-iban (subs iban 8 18)))
    false))

(defn new-token
  []
  (clojure.string/join (repeatedly 20 #(rand-int 10))))

(defn uuid->bytes
  [^UUID uuid]
  (let [bb (ByteBuffer/wrap (make-array Byte/TYPE 16))]
    (.putLong bb (.getMostSignificantBits uuid))
    (.putLong bb (.getLeastSignificantBits uuid))
    (.array bb)))

(defn bytes->uuid
  [^bytes bytes]
  (let [bb (ByteBuffer/wrap bytes)
        high (.getLong bb)
        low (.getLong bb)]
    (UUID. high low)))