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

(defn parse-all []
  (when (fs/exists? target-dir)
    (println (str "Cleaning " target-dir))
    (fs/delete-tree target-dir))
  (fs/create-dir target-dir)
  {:pylon (bluepylons/parse target-dir)
   :haata (haata/parse target-dir)
   :goat (theremingoat/parse target-dir)})

(defn scan-switches [source]
  (into {} (map (fn [filepath]
                  [(str (fs/file-name filepath))
                   {:source source}])
                (fs/glob target-dir
                         (str \* (file-postfix source))))))
  
(defn prepare []
  (spit (fs/file target-dir "metadata.edn")
        (pr-str {:date (java.util.Date.)
                 :sources sources
                 :reports (parse-all)
                 :switches (merge (scan-switches :pylon)
                                  (scan-switches :haata)
                                  (scan-switches :goat))})))
