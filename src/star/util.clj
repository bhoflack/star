(ns star.util)

(defn random-uuid [] (java.util.UUID/randomUUID))

(defmulti convert-uuid (fn [ds _] (:subprotocol ds)))
(defmethod convert-uuid "hsqldb" [ds value] (.toString value))
(defmethod convert-uuid :default [ds value] value)

(defn convert-uuids
  [ds values]
  (->> values             
       (map (fn [[k v]]
              (if (= java.util.UUID (type v))
                [k (convert-uuid ds v)]
                [k v])))
       (into {})))
