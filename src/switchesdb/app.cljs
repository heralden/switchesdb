(ns ^:figwheel-hooks switchesdb.app
  (:require [dumdom.core :as dumdom]
            [switchesdb.components :refer [App]]
            [cljs.reader :as edn]))

(def initial-state
  {:analyses []
   :switches-added #{}})

(defonce metadata (atom nil))
(defonce store (atom initial-state))
(defonce container (.getElementById js/document "app"))

(defn ^:after-load render [& _]
  (dumdom/render (App {:metadata @metadata
                       :state @store})
                 container))

(add-watch store ::me render)

(defmulti handle (fn [_state [action-name & _action-args]] action-name))

(defmethod handle :analyses/toggle-switch [state [_ switch-name]]
  (let [present? (contains? (:switches-added state) switch-name)]
    (-> state
        (update :switches-added (if present? disj conj) switch-name)
        (update :analyses (fn [analyses]
                            (if present?
                              (filterv #(not= switch-name (:name %)) analyses)
                              (conj analyses {:name switch-name})))))))

(defn find-analysis-index [analyses switch-name]
  (some (fn [[i analysis]]
          (when (= switch-name (:name analysis)) i))
        (map-indexed vector analyses)))

(defn vec-swap-indices [v i1 i2]
  (if (= i1 i2)
    v
    (let [e1 (nth v i1)
          e2 (nth v i2)]
      (-> v
          (assoc i1 e2)
          (assoc i2 e1)))))

(defmethod handle :analyses/move-up [{:keys [analyses] :as state} [_ switch-name]]
  (let [index (find-analysis-index analyses switch-name)]
    (if (zero? index)
      state
      (update state :analyses vec-swap-indices index (dec index)))))

(defmethod handle :analyses/move-down [{:keys [analyses] :as state} [_ switch-name]]
  (let [index (find-analysis-index analyses switch-name)]
    (if (= index (dec (count analyses)))
      state
      (update state :analyses vec-swap-indices index (inc index)))))

(defmethod handle :analyses/remove [state [_ switch-name]]
  (update state :analyses (fn [analyses]
                            (filterv #(not= switch-name (:name %)) analyses))))

(defonce only-once
  (-> (.fetch js/window "data/metadata.edn")
      (.then (fn [res] (.text res)))
      (.then (fn [edn-string]
               (reset! metadata (edn/read-string edn-string))
               (dumdom/set-event-handler! (fn [_event action]
                                            (swap! store handle action)))
               (render)))))

(comment
  (set! dumdom.component/*render-eagerly?* true)
  (set! dumdom.component/*render-comments?* true))
