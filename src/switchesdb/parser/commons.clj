(ns switchesdb.parser.commons
  (:refer-clojure :exclude [force])
  (:require [clojure.java.io :as io]
            [clojure.data.csv :as csv]))

(def csv-headers ["displacement" "force" "stroke"])

(defn data-row?
  [[displacement force stroke]]
  (and (number? displacement)
       (number? force)
       (string? stroke)))

(defn displacement [v] (nth v 0))
(defn force [v] (nth v 1))
(defn stroke [v] (nth v 2))
(defn measurement [displacement force stroke] [displacement force stroke])

(defn ignore? [v]
  (< (force v) 0.5))

(defn deduct [margin v]
  (measurement (- (displacement v) margin) (force v) (stroke v)))

(defn builder
  "Takes raw force-distance measurement data separated into the downstroke and upstroke,
  and attempts to clean and adjust it, returning a sequence of valid measurements.
  Measurements with a force of less than 0.5gf are ignored, and the displacement until
  this threshold is reached is deducted from all measurements. This means both cases
  of displacement being negative when force grows, and displacement being greater than
  zero when force grows, are compensated for."
  [downstroke upstroke]
  (let [downstroke (drop-while (complement data-row?) downstroke)
        upstroke (drop-while (complement data-row?) upstroke)
        _ (assert (seq downstroke) "No valid downstroke data")
        _ (assert (seq upstroke) "No valid upstroke data")
        margin (displacement (last (take-while ignore? downstroke)))
        margin? (and (some? margin) (not (zero? margin)))]
    (when margin?
      (println "INFO Adjusted by" margin "mm"))
    (cond->> (concat
               (take-while data-row? (drop-while ignore? downstroke))
               (take-while data-row? upstroke))
      margin? (map (partial deduct margin)))))

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
    (if existed? :overwritten :written)))
