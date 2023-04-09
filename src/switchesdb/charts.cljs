(ns switchesdb.charts
  (:require [switchesdb.utils :as utils]
            [clojure.string :as str]))

(defn embed-vega-lite [elem spec]
  (let [opts {:renderer :canvas
              :mode "vega-lite"
              :theme "quartz"
              :scaleFactor 2
              :downloadFileName (str "switchesdb_" (Math/abs (hash spec)))}]
    (-> (js/vegaEmbed elem (clj->js spec) (clj->js opts))
        (.then (get opts :callback #()))
        (.catch (fn [err]
                  (.warn js/console err))))))

;; category20
(def colors
  ["#1f77b4" "#aec7e8" "#ff7f0e" "#ffbb78" "#2ca02c" "#98df8a" "#d62728" "#ff9896" "#9467bd" "#c5b0d5" "#8c564b" "#c49c94" "#e377c2" "#f7b6d2" "#7f7f7f" "#c7c7c7" "#bcbd22" "#dbdb8d" "#17becf" "#9edae5"])

(defn layer-spec [{:keys [csv-file source display-name switch-name]}]
  {:data {:url (str "data/" csv-file)}
   :transform
   [{:filter {:field "displacement"
              :range [0 nil]}}
    {:filter {:field "force"
              :range [0 120]}}
    {:calculate (str "'" display-name "'") :as "Switch"}
    {:calculate (str "datum.stroke == 'up' ? '" switch-name "up' : '" switch-name "'") :as "ColorDomain"}
    {:calculate (str "'" (:author source) "'") :as "Source"}]
   :mark {:type "line"}
   :encoding
   {:x {:field "displacement"
        :title "Displacement (mm)"
        :type "quantitative"}
    :y {:field "force"
        :title "Force (gf)"
        :type "quantitative"}
    :color {:field "ColorDomain"
            :type "nominal"}
    :tooltip [{:field "displacement" :title "Displacement (mm)" :type "quantitative"}
              {:field "force" :title "Force (gf)" :type "quantitative"}
              {:field "stroke" :type "nominal"}
              {:field "Switch" :type "nominal"}
              {:field "Source" :type "nominal"}]}})

(defn force-curve-spec [{:keys [switches sources] :as _metadata} csv-files
                        & {:keys [hide-upstroke?]}]
  (let [map-clean #(map (partial utils/clean-switch-name+source sources) %)]
    {:$schema "https://vega.github.io/schema/vega-lite/v5.json"
     :width "container"
     :height 300
     :autosize {:type "fit-x"
                :contains "padding"}
     :encoding {:color {:title ""
                        :legend {:values (concat (map-clean csv-files)
                                                 [(str "switchesdb.com - measurements by "
                                                       (str/join ", " (map :author (vals sources))))])
                                 :orient "top-left"
                                 :labelLimit 0}
                        :scale {:domain (cond->> (mapcat (fn [s] [s (str s "up")]) (map-clean csv-files))
                                          hide-upstroke? (take-nth 2))
                                :range (cond->> (take (* 2 (count csv-files)) (cycle colors))
                                         hide-upstroke? (take-nth 2))}}
                :opacity {:value 0.8}}
     :layer (for [csv-file csv-files
                  :let [switch-meta (get switches csv-file)
                        source-meta (get sources (:source switch-meta))]]
              (layer-spec {:csv-file csv-file
                           :source source-meta
                           :display-name (utils/clean-switch-name csv-file)
                           :switch-name (utils/clean-switch-name+source sources csv-file)}))}))

(comment

  "For a watermark, add layer:"
  {:data {:values [{}]}
   :mark
   {:type "text"
    :text "my watermark"
    :fontSize 16
    :fill "#ccc"}})
