(ns switchesdb.parser.commons
  (:require [clojure.java.io :as io]
            [clojure.data.csv :as csv]))

(def csv-headers ["displacement" "force" "stroke"])

(defn data-row?
  [[displacement force stroke]]
  (and (number? displacement)
       (number? force)
       (string? stroke)))

(defn builder
  [downstroke upstroke]
  (concat
    (take-while data-row? downstroke)
    (take-while data-row? upstroke)))

(defn writer
  [data filename]
  (let [target (io/file "resources" "data" filename)
        existed? (.exists target)]
    (if existed?
      (println "WARNING Overwriting:" (str target))
      (println "Writing:" (str target)))
    (with-open [file-writer (io/writer target)]
      (csv/write-csv file-writer
                     (cons csv-headers data)))
    (if existed?
      :overwritten
      :written)))
