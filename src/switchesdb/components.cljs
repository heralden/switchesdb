(ns switchesdb.components
  (:require [dumdom.core :refer [defcomponent]]))

(defn embed-vega-lite [elem spec]
  (let [opts {:renderer :canvas
              :mode "vega-lite"
              :theme "quartz"}]
    (-> (js/vegaEmbed elem (clj->js spec) (clj->js opts))
        (.then (get opts :callback #()))
        (.catch (fn [err]
                  (.warn js/console err))))))

(defcomponent VegaLite
  :on-mount (fn [elem spec] (embed-vega-lite elem spec))
  :on-update (fn [& args]
               (.log js/console (pr-str args))
               (embed-vega-lite (first args) (second args)))
  [_spec]
  [:div "Loading chart..."])

(defn goat-spec [csv-file]
  {:$schema "https://vega.github.io/schema/vega-lite/v5.json"
   :data {:url csv-file}
   :transform
   [{:filter {:field "Force"
              :range [0 120]}}
    {:joinaggregate [{:op "argmax"
                      :field "Displacement"
                      :as "argmax_Displacement"}]}
    {:calculate "if(parseInt(datum['No.']) > parseInt(datum.argmax_Displacement['No.']), 'upstroke', 'downstroke')" :as "stroke"}]
   :mark "line"
   :encoding
   {:x {:field "Displacement"
        :title "Displacement (mm)"
        :type "quantitative"}
    :y {:field "Force"
        :title "Force (gf)"
        :type "quantitative"}
    :color {:field "stroke"}}})

(defcomponent App [{:keys []}]
  [:div
   [:h1 "foobar"]
   (VegaLite (goat-spec "data/Asus Rog NX Red Raw Data CSV.csv"))])
