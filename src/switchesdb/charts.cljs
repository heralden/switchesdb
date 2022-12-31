(ns switchesdb.charts)

(defn embed-vega-lite [elem spec]
  (let [opts {:renderer :canvas
              :mode "vega-lite"
              :theme "quartz"}]
    (-> (js/vegaEmbed elem (clj->js spec) (clj->js opts))
        (.then (get opts :callback #()))
        (.catch (fn [err]
                  (.warn js/console err))))))

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
   :width 500
   :height 250
   :mark {:type "line"
          :tooltip true}
   :encoding
   {:x {:field "Displacement"
        :title "Displacement (mm)"
        :type "quantitative"}
    :y {:field "Force"
        :title "Force (gf)"
        :type "quantitative"}
    :color {:field "stroke"
            :legend false
            :scale {:scheme "category20"}}}})

