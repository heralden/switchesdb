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
  :on-render (fn [elem spec old-spec]
               (when (not= spec old-spec)
                 (embed-vega-lite elem spec)))
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

(defn clean-switch-name [s]
  (str/replace s #"Raw Data CSV.csv$" ""))

(defcomponent SwitchesList [{:keys [switches analyses]}]
  (into [:ul.switches-list]
        (for [[switch-name _switch-details]
              (sort-by (comp str/lower-case key) switches)]
          [:li.switches-list-item
           [:div.dropdown
            [:button {:on-click [:analyses/new switch-name]} "add"]
            (clean-switch-name switch-name)
            ;; TODO this *for all switches* has to re-render whenever analyses changes.
            ;; hover is also not a thing on touch interfaces. have on-click add the element instead.
            (when (seq analyses)
              [:div.dropdown-content
               (into [:ul.dropdown-list]
                     (concat
                       (for [{[first-switch] :switches id :id} analyses]
                         [:li.dropdown-list-item
                          {:on-click [:analyses/add-switch switch-name id]}
                          (clean-switch-name first-switch)])
                       [[:li.dropdown-list-divider [:hr]]]
                       [[:li.dropdown-list-item.dropdown-list-item-new
                         {:on-click [:analyses/new switch-name]}
                         "New"]]))])]])))

(defcomponent SidePanel [{:keys [metadata state]}]
  [:aside.side-panel
   [:h1.side-title "SwitchesDB"]
   (SwitchesList {:switches (:switches metadata)
                  :analyses (:analyses state)})
   [:footer.side-footer
    "About"]])

(defcomponent Analysis [{:keys [id switches]}]
  [:section {:key id}
   [:div.analysis-controls
    [:button {:on-click [:analyses/move-up id]} "up"]
    [:button {:on-click [:analyses/move-down id]} "down"]
    [:button {:on-click [:analyses/remove id]} "del"]
    " : "
    (interpose " | "
      (for [switch-name switches]
        [:strong (clean-switch-name switch-name)
         [:button {:on-click (if (= 1 (count switches))
                               [:analyses/remove id]
                               [:analyses/remove-switch switch-name id])}
          "x"]]))]
   ;; TODO add overlays for additional switches
   (VegaLite (goat-spec (str "data/" (first switches))))])

(defcomponent Analyses [{:keys [state]}]
  [:main.analyses
   (for [analysis (:analyses state)]
     (Analysis analysis))])

(defcomponent App [{:keys [metadata state] :as input}]
  [:div.container
   (SidePanel input)
   (Analyses input)])
