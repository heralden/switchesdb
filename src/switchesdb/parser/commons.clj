(ns switchesdb.parser.commons
  (:refer-clojure :exclude [force])
  (:require [clojure.java.io :as io]
            [clojure.data.csv :as csv]))

(def csv-headers ["displacement" "force" "stroke"])

(defn displacement [v] (nth v 0))
(defn force [v] (nth v 1))
(defn stroke [v] (nth v 2))
(defn measurement [displacement force stroke] [displacement force stroke])

(defn data-row?
  [v]
  (and (number? (displacement v))
       (number? (force v))
       (or (= "up" (stroke v))
           (= "down" (stroke v)))))

(defn ignore? [v]
  (< (force v) 10))

(defn deduct [margin v]
  (measurement (- (displacement v) margin) (force v) (stroke v)))

(defn builder
  "Takes raw force-distance measurement data separated into the downstroke and upstroke,
  and attempts to clean and adjust it, returning a sequence of valid measurements.
  Measurements with a force of less than 10gf are ignored, and the displacement until
  this threshold is reached is deducted from all measurements. This means both cases
  of displacement being negative when force grows, and displacement being greater than
  zero when force grows, are compensated for. Finally, measurements with negative
  displacement are removed."
  [downstroke upstroke & {:keys [filename]}]
  (let [downstroke (drop-while (complement data-row?) downstroke)
        upstroke (drop-while (complement data-row?) upstroke)
        _ (assert (seq downstroke) "No valid downstroke data")
        _ (assert (seq upstroke) "No valid upstroke data")
        margin (displacement (last (take-while ignore? downstroke)))
        margin? (and (some? margin) (not (zero? margin)))]
    (when margin?
      (println "INFO Adjusted" filename "by" (- margin) "mm"))
    (cond->> (concat
               (take-while data-row? (drop-while ignore? downstroke))
               (take-while data-row? upstroke))
      margin? (map (partial deduct margin))
      :always (remove (comp neg? displacement)))))

(defn writer
  [data target-dir filename]
  (let [target (io/file target-dir filename)
        existed? (.exists target)]
    (if existed?
      (println "WARNING Overwriting:" (str target))
      (println "Writing:" (str target)))
    (with-open [file-writer (io/writer target)]
      (csv/write-csv file-writer
                     (cons csv-headers data)))
    (if existed? :overwritten :written)))

(comment
  "The 'dead-zone' of 10gf was reached by gradually increasing it until there
  were no more vastly incorrect adjustments done. While rare, there are switch
  measurements with jumps in force as much as 8gf, long before the key actually
  gets depressed. See HaaTa's measurement of Matias Linear.

  Other interesting findings on the source data:
  - Haata's data has been adjusted, obvious by the negative displacement values,
    but there are still varying degrees of pre-travel
  - Theremingoat's data have been adjusted in their graphs, but not in the CSV
  - Bluepylon's data has been well corrected
  
  10gf may be a high point for the graph to start, but ultimately we want the
  graphs to initiate when the key has truly began being depressed. We could add
  an initial zero data point, and get a vertical line from zero until the first
  measurement, albeit this may not be reason enough for an artifical data point.")
