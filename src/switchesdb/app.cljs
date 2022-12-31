(ns ^:figwheel-hooks switchesdb.app
  (:require [dumdom.core :as dumdom]
            [switchesdb.components :refer [App]]
            [switchesdb.handler :refer [handle]]
            [cljs.reader :as edn]))

(def initial-state
  {:analyses []})
;; {:analyses [{:id STR :switches [STR ...]} ...)}

(defonce metadata (atom nil))
(defonce store (atom initial-state))
(defonce container (.getElementById js/document "app"))

(defn ^:after-load render [& _]
  (dumdom/render (App {:metadata @metadata
                       :state @store})
                 container))

(add-watch store ::me render)

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
