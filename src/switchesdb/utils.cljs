(ns switchesdb.utils
  (:require [clojure.string :as str]
            [clojure.set :refer [map-invert]]
            [switchesdb.shared :refer [postfixes file-postfix-re]]))

(defn clean-switch-name [filename]
  (str/replace filename file-postfix-re ""))

(defn clean-switch-name+source [sources-meta filename]
  (let [[_ source] (re-find file-postfix-re filename)
        {:keys [author]} (sources-meta ((map-invert postfixes) source))]
    (str/replace filename file-postfix-re (str " [" author "]"))))

(defn ->filterf [{:keys [text] :as _filters} keyfn]
  (comp
    (if (str/blank? text)
      identity
      ;; Fuzzy search: string needs to contain every word someplace.
      (let [keyws (-> text str/trim str/lower-case (str/split #"\s+"))]
        (partial filter (fn [x]
                          (every? #(str/includes? (keyfn x) %) keyws)))))))
