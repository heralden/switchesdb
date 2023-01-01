(ns switchesdb.charts
  (:require [switchesdb.utils :as utils]))

(defn embed-vega-lite [elem spec]
  (let [opts {:renderer :canvas
              :mode "vega-lite"
              :theme "quartz"}]
    (-> (js/vegaEmbed elem (clj->js spec) (clj->js opts))
        (.then (get opts :callback #()))
        (.catch (fn [err]
                  (.warn js/console err))))))

;; category20
(def colors
  ["#1f77b4" "#aec7e8" "#ff7f0e" "#ffbb78" "#2ca02c" "#98df8a" "#d62728" "#ff9896" "#9467bd" "#c5b0d5" "#8c564b" "#c49c94" "#e377c2" "#f7b6d2" "#7f7f7f" "#c7c7c7" "#bcbd22" "#dbdb8d" "#17becf" "#9edae5"])

(defn goat-spec [{:keys [csv-file color1 color2]}]
  {:data {:url (str "data/" csv-file)}
   :transform
   [{:filter {:field "Displacement"
              :range [0 nil]}}
    {:filter {:field "Force"
              :range [0 120]}}
    {:joinaggregate [{:op "argmax"
                      :field "Displacement"
                      :as "argmax_Displacement"}]}
    {:calculate "if(parseInt(datum['No.']) > parseInt(datum.argmax_Displacement['No.']), 'upstroke', 'downstroke')" :as "stroke"}
    {:calculate (str "datum.stroke == 'downstroke' ? '" color1 "' : '" color2 "'") :as "color"}
    {:calculate (str "'" (utils/clean-switch-name csv-file) "'") :as "switch"}]
   :mark {:type "line"}
   :encoding
   {:x {:field "Displacement"
        :title "Displacement (mm)"
        :type "quantitative"}
    :y {:field "Force"
        :title "Force (gf)"
        :type "quantitative"}
    :color {:field "color"
            :type "nominal"
            :legend false
            :scale nil}
    :tooltip [{:field "Displacement" :title "Displacement (mm)" :type "quantitative"}
              {:field "Force" :title "Force (gf)" :type "quantitative"}
              {:field "switch" :type "nominal"}
              {:field "stroke" :type "nominal"}]}})

(defn force-curve-spec [switches-metadata csv-files]
  {:$schema "https://vega.github.io/schema/vega-lite/v5.json"
   :width 500
   :height 250
   :layer (for [[index csv-file] (map-indexed vector csv-files)
                :let [metadata (get switches-metadata csv-file)
                      [color1 color2] (drop (* 2 index) (cycle colors))]]
            (case (:source metadata)
              :goat (goat-spec {:csv-file csv-file
                                :color1 color1
                                :color2 color2})))})
