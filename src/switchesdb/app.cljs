(ns switchesdb.app
  (:require [dumdom.core :as dumdom]
            [switchesdb.components :refer [App]]))

(defonce container (.getElementById js/document "app"))

(defn render [& _]
  (dumdom/render (App {}) container))

(render)
