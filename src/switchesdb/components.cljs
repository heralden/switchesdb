(ns switchesdb.components
  (:require [dumdom.core :refer [defcomponent]]
            [clojure.string :as str]))

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
   ; :width "container"
   :mark "line"
   :encoding
   {:x {:field "Displacement"
        :title "Displacement (mm)"
        :type "quantitative"}
    :y {:field "Force"
        :title "Force (gf)"
        :type "quantitative"}
    :color {:field "stroke"
            :legend false}}})

(defcomponent SwitchesList [{:keys [switches]}]
  (into [:ul.switches-list]
        (for [[switch-name _switch-details]
              (sort-by (comp str/lower-case key) switches)]
          [:li [:label
                [:input {:type "checkbox"}]
                switch-name]])))

(defcomponent SidePanel [{:keys [metadata store]}]
  [:aside.side-panel
   [:h1.side-title "SwitchesDB"]
   (SwitchesList {:switches (:switches metadata)})
   [:footer.side-footer
    "About"]])

(defcomponent Analyses [{:keys []}]
  [:main.analyses
   [:section
    (VegaLite (goat-spec "data/Asus Rog NX Red Raw Data CSV.csv"))]])

(defcomponent App [{:keys [metadata store] :as state}]
  [:div.container
   (SidePanel state)
   (Analyses state)])
