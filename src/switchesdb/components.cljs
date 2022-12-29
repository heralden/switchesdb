(ns switchesdb.components
  (:require [dumdom.core :refer [defcomponent]]))

(defcomponent App [{:keys []}]
  [:h1 "foobar"])
