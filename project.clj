(defproject star "0.1.0"
  :description "A Clojure library to write slowly changing dimensions and fact tables for star schemas."
  :url "http://github.com/bhoflack/star"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.7.0"]
                 [org.clojure/java.jdbc "0.4.1"]
                 [org.clojure/tools.logging "0.3.1"]
                 [clj-time "0.11.0"]

                 ; test dependencies
                 [org.hsqldb/hsqldb "2.3.3"]])
