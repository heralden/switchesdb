(ns ^:figwheel-hooks switchesdb.app
  (:require [dumdom.core :as dumdom]
            [switchesdb.components :refer [App]]
            [cljs.reader :as edn]))

(defonce metadata (atom nil))
(defonce store (atom nil))
(defonce container (.getElementById js/document "app"))

(defn ^:after-load render [& _]
  (dumdom/render (App {:metadata @metadata
                       :store @store})
                 container))

(defonce only-once
  (-> (.fetch js/window "data/metadata.edn")
      (.then (fn [res] (.text res)))
      (.then (fn [edn-string]
               (reset! metadata (edn/read-string edn-string))
               (render)))))
