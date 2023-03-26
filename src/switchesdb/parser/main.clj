(ns switchesdb.parser.main
  (:require [switchesdb.parser.bluepylons :as bluepylons]
            [switchesdb.parser.haata :as haata]
            [switchesdb.parser.theremingoat :as theremingoat]
            [babashka.fs :as fs]
            [switchesdb.shared :refer [file-postfix]]))

(def target-dir (fs/file "resources" "data"))

(def sources
  {:pylon {:author "bluepylons"
           :url "https://github.com/bluepylons/Open-Switch-Curve-Meter"}
   :haata {:author "HaaTa"
           :url "https://plot.ly/~haata"}
   :goat {:author "ThereminGoat"
          :url "https://github.com/ThereminGoat/force-curves"}})

(defn spit-logs [out-file f]
  (let [logs (java.io.StringWriter.)]
    (binding [*out* logs]
      (let [return (f)]
        (spit out-file logs)
        return))))

(defn parse-all []
  (when (fs/exists? target-dir)
    (println (str "Cleaning " target-dir))
    (fs/delete-tree target-dir))
  (fs/create-dir target-dir)
  (let [_ (println "Parsing" (get-in sources [:pylon :author]) "data")
        pylon-report (spit-logs (fs/file target-dir "pylon.log")
                                #(bluepylons/parse target-dir))
        _ (println "Parsing" (get-in sources [:haata :author]) "data")
        haata-report (spit-logs (fs/file target-dir "haata.log")
                                #(haata/parse target-dir))
        _ (println "Parsing" (get-in sources [:goat :author]) "data")
        goat-report (spit-logs (fs/file target-dir "goat.log")
                               #(theremingoat/parse target-dir))]
    (println "Done parsing!")
    {:pylon pylon-report
     :haata haata-report
     :goat goat-report}))

(defn scan-switches [source]
  (into {} (map (fn [filepath]
                  [(str (fs/file-name filepath))
                   {:source source}])
                (fs/glob target-dir
                         (str \* (file-postfix source))))))
  
(defn prepare []
  (let [reports (parse-all)
        _ (println "Generating metadata")
        switches (merge (scan-switches :pylon)
                        (scan-switches :haata)
                        (scan-switches :goat))]
    (spit (fs/file target-dir "metadata.edn")
          (pr-str {:date (java.util.Date.)
                   :sources sources
                   :reports reports
                   :switches switches}))
    (println "All done!")
    (prn reports)
    (println "Total:" (count switches) "switches outputted")))
