(ns ^:figwheel-hooks switchesdb.app
  (:require [dumdom.core :as dumdom]
            [switchesdb.components :refer [App]]
            [switchesdb.handler :refer [handle]]
            [cljs.reader :as edn]
            [clojure.string :as str]))

(def initial-state
  {:analyses [] ; [{:id STR :switches [STR ...]} ...)}
   :add-switch-dialog nil ; {:top INT :switch STR}
   :filters {:text ""}
   :mobile-side-panel? true})

(defonce metadata (atom nil))
(defonce store (atom initial-state))
(defonce container (.getElementById js/document "app"))

(defn save-hash! [analyses]
  (set! js/location.hash
        (str/join \; (map (fn [{:keys [switches]}]
                            (str/join \, (map js/encodeURIComponent switches)))
                          analyses))))

(defn load-hash! [{all-switches :switches :as _metadata}]
  (when-let [hash (not-empty (subs (.-hash js/location) 1))]
    (let [raw-analyses (try
                         (->> (str/split hash #";")
                              (map #(str/split % #","))
                              (map #(mapv js/decodeURIComponent %))
                              (mapv (fn [switches]
                                      {:id (gensym) :switches switches})))
                         (catch js/Object e
                           (.error js/console e)
                           (.error js/console "Failed to load state from hash")
                           []))
          invalid-switches (mapcat (fn [{raw-switches :switches}]
                                     (remove #(contains? all-switches %) raw-switches))
                                   raw-analyses)
          analyses (vec (keep (fn [{raw-switches :switches :as analysis}]
                                (when-let [switches (not-empty (filterv #(contains? all-switches %) raw-switches))]
                                  (assoc analysis :switches switches)))
                              raw-analyses))]
      (when (seq invalid-switches)
        (js/alert (str "Could not find switches in URL: " \newline
                       (str/join ", " (map #(str \" % \") invalid-switches)) \newline
                       "They have likely been renamed or removed.")))
      (swap! store assoc :analyses analyses))))

(defn ^:after-load render [& _]
  (dumdom/render (App {:metadata @metadata
                       :state @store})
                 container))

(defonce only-once
  (-> (.fetch js/window "data/metadata.edn")
      (.then (fn [res] (.text res)))
      (.then (fn [edn-string]
               (dumdom/set-event-handler! (fn [event action+]
                                            (if (and (seqable? action+) (vector? (first action+)))
                                              ;; Handle multiple events.
                                              (swap! store (apply comp (map #(fn [state]
                                                                               (handle state % event))
                                                                            (reverse action+))))
                                              (swap! store handle action+ event))))
               (load-hash! (reset! metadata (edn/read-string edn-string)))
               (render)))))

(add-watch store ::render render)
(add-watch store ::hash #(when (not= (:analyses %3) (:analyses %4))
                           (save-hash! (:analyses %4))))

(comment
  (set! dumdom.component/*render-eagerly?* true)
  (set! dumdom.component/*render-comments?* true))
