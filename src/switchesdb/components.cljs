(ns switchesdb.components
  {:clj-kondo/config '{:lint-as {dumdom.core/defcomponent clojure.core/defn}}}
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

#_:clj-kondo/ignore
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

(defn clean-switch-name [s]
  (str/replace s #"Raw Data CSV.csv$" ""))

(defcomponent SwitchesList [{:keys [switches switches-added]}]
  (into [:ul.switches-list]
        (for [[switch-name _switch-details]
              (sort-by (comp str/lower-case key) switches)]
          [:li.switches-list-item
           [:label
            [:input {:type "checkbox"
                     :checked (contains? switches-added switch-name)
                     :on-change [:analyses/toggle-switch switch-name]}]
            (clean-switch-name switch-name)]])))

(defcomponent SidePanel [{:keys [metadata state]}]
  [:aside.side-panel
   [:h1.side-title "SwitchesDB"]
   (SwitchesList {:switches (:switches metadata)
                  :switches-added (:switches-added state)})
   [:footer.side-footer
    "About"]])

(defcomponent Analysis [{switch-name :name}]
  [:section {:key switch-name}
   (VegaLite (goat-spec (str "data/" switch-name)))])

(defcomponent Analyses [{:keys [state]}]
  [:main.analyses
   (for [analysis (:analyses state)]
     (Analysis analysis))])

(defcomponent App [{:keys [metadata state] :as input}]
  [:div.container
   (SidePanel input)
   (Analyses input)])
