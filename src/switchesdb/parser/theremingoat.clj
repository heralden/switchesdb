(ns switchesdb.parser.theremingoat
  (:require [clojure.string :as str]
            [babashka.fs :as fs]
            [clojure.java.io :as io]
            [clojure.data.csv :refer [read-csv]]
            [switchesdb.parser.commons :refer [builder writer]]
            [switchesdb.shared :refer [file-postfix]]))

(defn target-filename [csv-path]
  (-> (fs/file-name csv-path)
      (str/replace #" Raw Data CSV\.csv$" (file-postfix :goat))))

(defn reader [file-reader csv-path]
  (let [values (read-csv file-reader)
        [_ return-point] (re-matches #"No\.(\d+)" (nth (first values) 3))
        downstroke (map (fn [[_ force _ displacement]]
                          [(parse-double displacement)
                           (parse-double force)
                           "down"])
                        (take-while #(not= (first %) return-point)
                                    (drop 6 values)))
        upstroke (map (fn [[_ force _ displacement]]
                        [(parse-double displacement)
                         (parse-double force)
                         "up"])
                      (drop-while #(not= (first %) return-point)
                                  (drop 6 values)))]
    (builder downstroke upstroke
             :filename (target-filename csv-path))))

(defn parse [target-dir]
  (let [filepaths (fs/glob "resources/theremingoat" "**Raw Data CSV.csv")
        results (mapv (fn [csv-path]
                        (try
                          (with-open [file-reader (io/reader (fs/file csv-path))]
                            (writer (reader file-reader csv-path)
                                    target-dir
                                    (target-filename csv-path)))
                          (catch Throwable e
                            (println "ERROR Parsing CSV file" (fs/file-name csv-path)
                                     "resulted in exception:" (ex-message e))
                            :invalid)))
                      filepaths)]
    (assoc (frequencies results)
           :filecount (count filepaths)))) 

(comment
  (with-open [file-reader (io/reader (io/resource "theremingoat/Forgiven/Forgiven Raw Data CSV.csv"))]
    (doall (reader file-reader))))
