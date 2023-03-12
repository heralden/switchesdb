(ns switchesdb.parser.haata
  (:require [clojure.string :as str]
            [babashka.fs :as fs]
            [clojure.java.io :as io]
            [clojure.data.csv :refer [read-csv]]
            [switchesdb.parser.commons :refer [builder writer]]))

(defn reader [file-reader]
  (let [values (read-csv file-reader)
        downstroke (map (fn [[displacement force]]
                          [(parse-double displacement)
                           (parse-double force)
                           "down"])
                        (drop 1 values))
        upstroke (map (fn [[_ _ displacement force]]
                        [(parse-double displacement)
                         (parse-double force)
                         "up"])
                      (drop 1 values))]
    (builder downstroke upstroke)))

(defn target-filename [csv-path]
  (fs/file-name csv-path))

(defn parse []
  (let [filepaths (fs/glob "resources/haata" "*.csv")
        results (mapv (fn [csv-path]
                        (try
                          (with-open [file-reader (io/reader (fs/file csv-path))]
                            (writer (reader file-reader)
                                    (target-filename csv-path)))
                          (catch Throwable e
                            (println "ERROR Parsing CSV file" (fs/file-name csv-path)
                                     "resulted in exception:" (ex-message e))
                            :invalid)))
                      filepaths)]
    (assoc (frequencies results)
           :filecount (count filepaths)))) 

(comment
  (with-open [file-reader (io/reader (io/resource "haata/Alps SKCC Cream.csv"))]
    (doall (reader file-reader))))
