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

(defn goat-spec [{:keys [csv-file color1 color2 source]}]
  {:data {:url (str "data/" csv-file)}
   :transform
   [{:filter {:field "Displacement"
              :range [0 nil]}}
    {:filter {:field "Force"
              :range [0 120]}}
    {:joinaggregate [{:op "argmax"
                      :field "Displacement"
                      :as "argmax_Displacement"}]}
    {:calculate "if(parseInt(datum['No.']) > parseInt(datum.argmax_Displacement['No.']), 'upstroke', 'downstroke')" :as "Stroke"}
    {:calculate (str "datum.Stroke == 'downstroke' ? '" color1 "' : '" color2 "'") :as "color"}
    {:calculate (str "'" (utils/clean-switch-name csv-file) "'") :as "Switch"}
    {:calculate (str "'" (:author source) "'") :as "Source"}]
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
              {:field "Stroke" :type "nominal"}
              {:field "Switch" :type "nominal"}
              {:field "Source" :type "nominal"}]}})

(defn haata-spec [{:keys [csv-file color1 color2 source]}]
  {:data {:url (str "data/" csv-file)}
   :layer [{:transform
            [{:filter {:field "Press 1, x"
                       :range [0 nil]}}
             {:filter {:field "Press 1, y"
                       :range [0 120]}}
             {:calculate "'downstroke'" :as "Stroke"}
             {:calculate (str "'" (utils/clean-switch-name csv-file) "'") :as "Switch"}
             {:calculate (str "'" (:author source) "'") :as "Source"}]
            :mark {:type "line"}
            :encoding
            {:x {:field "Press 1, x"
                 :title "Displacement (mm)"
                 :type "quantitative"}
             :y {:field "Press 1, y"
                 :title "Force (gf)"
                 :type "quantitative"}
             :color {:legend false
                     :value color1}
             :tooltip [{:field "Press 1, x" :title "Displacement (mm)" :type "quantitative"}
                       {:field "Press 1, y" :title "Force (gf)" :type "quantitative"}
                       {:field "Stroke" :type "nominal"}
                       {:field "Switch" :type "nominal"}
                       {:field "Source" :type "nominal"}]}}
           {:transform
            [{:filter {:field "Release 1, x"
                       :range [0 nil]}}
             {:filter {:field "Release 1, y"
                       :range [0 120]}}
             {:calculate "'upstroke'" :as "Stroke"}
             {:calculate (str "'" (utils/clean-switch-name csv-file) "'") :as "Switch"}
             {:calculate (str "'" (:author source) "'") :as "Source"}]
            :mark {:type "line"}
            :encoding
            {:x {:field "Release 1, x"
                 :title "Displacement (mm)"
                 :type "quantitative"}
             :y {:field "Release 1, y"
                 :title "Force (gf)"
                 :type "quantitative"}
             :color {:legend false
                     :value color2}
             :tooltip [{:field "Release 1, x" :title "Displacement (mm)" :type "quantitative"}
                       {:field "Release 1, y" :title "Force (gf)" :type "quantitative"}
                       {:field "Stroke" :type "nominal"}
                       {:field "Switch" :type "nominal"}
                       {:field "Source" :type "nominal"}]}}]})

(defn force-curve-spec [{:keys [switches sources] :as _metadata} csv-files]
  {:$schema "https://vega.github.io/schema/vega-lite/v5.json"
   :width 500
   :height 250
   ; :autosize {:type "fit-x"
   ;            :contains "padding"}
   :layer (for [[index csv-file] (map-indexed vector csv-files)
                :let [metadata (get switches csv-file)
                      [color1 color2] (drop (* 2 index) (cycle colors))]]
            (case (:source metadata)
              :goat (goat-spec {:csv-file csv-file
                                :color1 color1
                                :color2 color2
                                :source (get sources :goat)})
              :haata (haata-spec {:csv-file csv-file
                                  :color1 color1
                                  :color2 color2
                                  :source (get sources :haata)})))})
