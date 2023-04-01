(ns switchesdb.shared)

(def postfixes
  {:pylon "BP"
   :haata "HT"
   :goat "TG"})

(defn file-postfix [source]
  (assert (contains? postfixes source))
  (str \~ (postfixes source) ".csv"))

(def file-postfix-re
  (re-pattern (str "~(" (apply str (interpose \| (vals postfixes))) ")"
                   ".csv$")))
