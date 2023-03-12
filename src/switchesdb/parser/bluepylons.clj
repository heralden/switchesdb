(ns switchesdb.parser.bluepylons
  (:require [clojure.string :as str]
            [babashka.fs :as fs]
            [switchesdb.parser.commons :refer [builder writer]])
  (:import [com.github.miachm.sods SpreadSheet NotAnOdsException]))

(defn- debug-log [msg values]
  (prn msg (map vec values)))

(defn target-filename [ods-path]
  (-> (fs/file-name ods-path)
      (str/replace #"\.ods$" ".csv")
      (str/replace #"[_-]" " ")))

(defn read-type-1
  [sheet ods-path]
  #_(debug-log "type-1" (-> sheet (.getRange 0 5 5 4) .getValues))
  (println "Reading type 1:" (str ods-path))
  (let [max-rows (.getMaxRows sheet)
        values (-> sheet (.getRange 2 5 (- max-rows 2) 4) .getValues)
        downstroke (map (fn [[displacement force _ _]]
                          [displacement force "down"])
                        values)
        upstroke (map (fn [[_ _ displacement force]]
                        [displacement force "up"])
                      values)]
    (builder downstroke upstroke
             :filename (target-filename ods-path))))

(defn read-type-2
  [sheet ods-path]
  #_(debug-log "type-2" (-> sheet (.getRange 0 5 5 6) .getValues))
  (println "Reading type 2:" (str ods-path))
  (let [max-rows (.getMaxRows sheet)
        values (-> sheet (.getRange 1 5 (- max-rows 1) 6) .getValues)
        downstroke (map (fn [[displacement force _ _ _ _]]
                          [displacement force "down"])
                        values)
        upstroke (map (fn [[_ _ _ displacement force _]]
                        [displacement force "up"])
                      values)]
    (builder downstroke upstroke
             :filename (target-filename ods-path))))

(defn read-type-3
  [sheet ods-path]
  #_(debug-log "type-3" (-> sheet (.getRange 0 4 5 4) .getValues))
  (println "Reading type 3:" (str ods-path))
  (let [max-rows (.getMaxRows sheet)
        values (-> sheet (.getRange 2 4 (- max-rows 2) 4) .getValues)
        downstroke (map (fn [[displacement force _ _]]
                          [displacement force "down"])
                        values)
        upstroke (map (fn [[_ _ displacement force]]
                        [displacement force "up"])
                      values)]
    (builder downstroke upstroke
             :filename (target-filename ods-path))))

(defn reader-fn
  "Where `head` is a vector of cells F1 and G1, return the suitable function."
  [head]
  (cond
    (= (second head) "Corrected Upstroke")
    read-type-3

    (= (first head) "Corrected downstroke X (mm)")
    read-type-2

    (and (string? (first head))
         (str/includes? (str/lower-case (first head)) "corrected downstroke"))
    read-type-1))

(defn read-head [sheet]
  (-> sheet (.getRange 0 5 1 2) .getValues first))

(defn read-ods [ods-path]
  (try
    (-> ods-path fs/file SpreadSheet. .getSheets (nth 0))
    (catch NotAnOdsException _
      (println "ERROR Failed to read ODS file:" (str ods-path)))))

(defn parse []
  (let [filepaths (fs/glob "resources/bluepylons/Force curve measurements" "{,Kailh Choc Switches/}*.ods")
        results (mapv (fn [ods-path]
                        (if-let [sheet (read-ods ods-path)]
                          (if-let [reader (reader-fn (read-head sheet))]
                            (try
                              (writer (reader sheet ods-path)
                                      (target-filename ods-path))
                              (catch Throwable e
                                (println "ERROR Parsing ODS file" (fs/file-name ods-path)
                                         "resulted in exception:" (ex-message e))
                                :invalid))
                            (do (println "WARNING Skipped due to unsupported sheet layout:" (str ods-path))
                                :unsupported))
                          :invalid))
                      filepaths)]
    (assoc (frequencies results)
           :filecount (count filepaths))))

(comment
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

  ;; Type 1 example
  [["Corrected Downstroke" nil "Corrected Upstroke (Aura)" nil]
   ["X (mm)" "Force (gf)" "X (mm)" "Force (gf)"]
   [0.0 0.446 3.29 59.416] [0.005 6.976 3.285 59.356] [0.01 21.436 3.28 59.296]]
  ;; Type 2 example
  [["Corrected downstroke X (mm)" "Corrected downstroke force (gf)" "Corrected downstroke actuated?"
    "Corrected upstroke X (mm)" "Corrected upstroke force (gf)" "Corrected upstroke actuated?"]
   [0.0 7.462 0.0 3.955 109.322 1.0] [0.005 13.822 0.0 3.95 102.732 1.0] [0.01 25.502 0.0 3.945 89.522 1.0] [0.015 30.492 0.0 3.94 83.632 1.0]]
  ;; Type 3 example
  [["Corrected Downstroke" nil "Corrected Upstroke" nil]
   ["X (mm)" "Force (gf)" "X (mm)" "Force (gf)"]
   [0.0 7.61 3.805 94.71] [0.005 15.02 3.8 86.78] [0.01 26.98 3.795 72.55]]

  :end)
