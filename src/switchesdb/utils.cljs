(ns switchesdb.utils
  (:require [clojure.string :as str]))

(defn clean-switch-name [s]
  (str/replace s #"\.csv$" ""))

(defn ->filterf [{:keys [text] :as _filters} keyfn]
  (comp
    (if (str/blank? text)
      identity
      ;; Fuzzy search: string needs to contain every word someplace.
      (let [keyws (-> text str/trim str/lower-case (str/split #"\s+"))]
        (partial filter (fn [x]
                          (every? #(str/includes? (keyfn x) %) keyws)))))))
