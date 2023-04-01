(ns switchesdb.utils
  (:require [clojure.string :as str]
            [switchesdb.shared :refer [postfixes]]))

(defn clean-switch-name [filename]
  (let [re (re-pattern (str "~(" (apply str (interpose \| (vals postfixes))) ")"
                            ".csv$"))]
    (str/replace filename re "")))

(defn ->filterf [{:keys [text] :as _filters} keyfn]
  (comp
    (if (str/blank? text)
      identity
      ;; Fuzzy search: string needs to contain every word someplace.
      (let [keyws (-> text str/trim str/lower-case (str/split #"\s+"))]
        (partial filter (fn [x]
                          (every? #(str/includes? (keyfn x) %) keyws)))))))
