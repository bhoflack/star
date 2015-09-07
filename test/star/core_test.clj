(ns star.core-test
  (:require [clojure.test :refer :all]
            [star.core :refer :all]
            [star.util :refer [random-uuid]]
            [clojure.java.jdbc :as j]))

(def ds {:classname "org.hsqldb.jdbcDriver"
         :subprotocol "hsqldb"
         :subname "mem:star"})

(defn init-schema [ds]
  (j/db-do-commands ds
                    "DROP TABLE fact_measurement IF EXISTS"
                    "DROP TABLE dim_ci IF EXISTS"
                    (j/create-table-ddl :dim_ci
                                        [:id "varchar(255)" "PRIMARY KEY"]
                                        [:memory :integer]
                                        [:hostname "varchar(255)"]
                                        [:tag "varchar(255)"]
                                        [:num_cpus :integer]                                        
                                        [:stopped_at :timestamp]
                                        [:created_at :timestamp])
                    (j/create-table-ddl :fact_measurement
                                        [:id "varchar(255)" "PRIMARY KEY"]
                                        [:ci_id "varchar(255)" "REFERENCES dim_ci(id)"]
                                        [:value :integer])))

(deftest slowly-changing-dimension-test
  (testing "Same id for identical key values"
    (init-schema ds)

    (let [initial-id (slowly-changing-dimension ds
                                                :dim_ci
                                                {:memory 2048 :hostname "zeus" :num_cpus 2 :tag "obsolete"}
                                                [:memory :hostname :num_cpus]
                                                [:hostname])
          equal-id (slowly-changing-dimension ds
                                              :dim_ci
                                              {:memory 2048 :hostname "zeus" :num_cpus 2 :tag "to be upgraded"}
                                              [:memory :hostname :num_cpus]
                                              [:hostname])]
      (is (= initial-id
             (java.util.UUID/fromString equal-id)))))

  (testing "An exiting dimension is stopped when it's obsolete"
    (init-schema ds)
    
    (let [initial-id (slowly-changing-dimension ds
                                                :dim_ci
                                                {:memory 2048 :hostname "zeus" :num_cpus 2 :tag "to be upgraded"}
                                                [:memory :hostname :num_cpus]
                                                [:hostname])
          upgraded-id (slowly-changing-dimension ds
                                                 :dim_ci
                                                 {:memory 4096 :hostname "zeus" :num_cpus 2 :tag "updated"}
                                                 [:memory :hostname :num_cpus]
                                                 [:hostname])]
      (is (not= initial-id upgraded-id))

                                        ; The old entry should be stopped
      (is (not (nil? (-> (j/query ds ["select stopped_at from dim_ci where id = ?" (.toString initial-id)])
                         first
                         :stopped_at))))

      (is (nil? (-> (j/query ds ["select stopped_at from dim_ci where id = ?" (.toString upgraded-id)])
                    first
                    :stopped_at))))))

(deftest insert-into-facttable-test
  (testing "Insert into the facttable"
    (init-schema ds)
    (let [fact-id (random-uuid)
          ci (slowly-changing-dimension ds
                                        :dim_ci
                                        {:memory 2048 :hostname "zeus" :num_cpus 2 :tag "to be upgraded"}
                                        [:memory :hostname :num_cpus]
                                        [:hostname])]
      (insert-into-facttable ds
                             :fact_measurement
                             {:id fact-id
                              :ci_id ci
                              :value 20})
      

      (is (not (nil? fact-id)))
      (is (= {:hostname "zeus"
              :value 20
              :memory 2048}
             (-> (j/query ds ["select ci.hostname, ci.memory, m.value
                                 from fact_measurement m
                                   inner join dim_ci ci on ci.id = m.ci_id
                                 where m.id = ?" (.toString fact-id)])
                 (first)))))))
