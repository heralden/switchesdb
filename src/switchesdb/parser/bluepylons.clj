(ns switchesdb.parser.bluepylons
  (:require [clojure.string :as str]
            [babashka.fs :as fs])
  (:import [com.github.miachm.sods SpreadSheet NotAnOdsException]))

(defn reader-fn
  "Where `head` is a vector of cells F1 and G1, return the suitable function."
  [head]
  (cond
    (str/includes? (str/lower-case (first head)) "corrected downstroke")
    'read-type-1
    (= (first head) "Corrected downstroke X (mm)")
    'read-type-2
    (= (second head) "Corrected Upstroke")
    'read-type-3))

(defn read-ods [ods-file]
  (try
    (-> ods-file fs/file SpreadSheet. .getSheets (nth 0) (.getRange 0 5 1 6) .getValues)
    (catch NotAnOdsException _
      (println (str "Failed to read ODS file: " ods-file)))))

(comment
  (fs/glob "resources/bluepylons/Force curve measurements" "{,Kailh Choc Switches/}*.ods")

  ;; There are 15 different arrangements the spreadsheets can be found in.
  (letfn [(read-ods-head [ods-file]
            (try (-> ods-file fs/file SpreadSheet. .getSheets (nth 0) (.getRange 0 5 1 2) .getValues)
                 (catch NotAnOdsException _)))]
    (frequencies
      (map #(vec (map vec %))
           (map read-ods-head
                (fs/glob "resources/bluepylons/Force curve measurements" "{,Kailh Choc Switches/}*.ods")
                #_(fs/glob "resources/bluepylons/Force curve measurements" "*.ods")
                #_(fs/glob "resources/bluepylons/Force curve measurements" "{,Kailh Choc Switches/,Springs/}*.ods")))))
  ;; The most common ones are:
  [["Corrected Downstroke" nil "Corrected Upstroke"]]
  ;; 150 times (labels may include extra text)
  [["Corrected downstroke X (mm)"
    "Corrected downstroke force (gf)"
    "Corrected downstroke actuated?"
    "Corrected upstroke X (mm)"
    "Corrected upstroke force (gf)"
    "Corrected upstroke actuated?"]]
  ;; 117 times
  [[nil "Corrected Upstroke"]]
  ;; 39 times (similar to first but offset by 1 column)

  ;; The remaining arrangements total 7, each one having its own unique arrangement.
  ;; Only the 3 most common arrangements shown above will be handled.

  :end)
