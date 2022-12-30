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

(defonce only-once
  (-> (.fetch js/window "data/metadata.edn")
      (.then (fn [res] (.text res)))
      (.then (fn [edn-string]
               (reset! metadata (edn/read-string edn-string))
               (dumdom/set-event-handler! (fn [_event action]
                                            (swap! store handle action)))
               (render)))))
