(ns switchesdb.components
  {:clj-kondo/config '{:lint-as {dumdom.core/defcomponent clojure.core/defn}}}
  (:require [dumdom.core :refer [defcomponent]]
            [clojure.string :as str]
            [switchesdb.charts :as charts]
            [switchesdb.utils :as utils]))

#_:clj-kondo/ignore
(defcomponent VegaLite
  :on-render (fn [elem spec old-spec]
               (when (not= spec old-spec)
                 (charts/embed-vega-lite elem spec)))
  [_spec]
  [:div "Loading chart..."])

(defcomponent SwitchesList [{:keys [switches filters]}]
  (let [switches (->> switches
                      ((utils/->filterf filters (comp str/lower-case key)))
                      (sort-by (comp str/lower-case key)))]
    [:ul.switches-list
     (if (seq switches)
       (for [[switch-name _switch-details] switches]
         [:li.switches-list-item
          [:button {:on-click [:analyses/new switch-name]} "+"]
          [:span.switches-list-name
           {:on-click [:switches/add-dialog switch-name]}
           (utils/clean-switch-name switch-name)]])
       "No results")]))

(defcomponent FilterBox [{:keys [text]}]
  [:div.filter-box
   [:input {:type "text"
            :placeholder "Filter switches"
            :on-input [:filters/set-text]
            :value text}]])

(defcomponent SidePanel [{:keys [metadata state]}]
  [:aside.side-panel
   [:h1.side-title "SwitchesDB"]
   (FilterBox (:filters state))
   (SwitchesList {:switches (:switches metadata)
                  :filters (:filters state)})
   [:footer.side-footer
    [:a {:href "https://github.com/heralden/switchesdb" :target "_blank"}
     "Source"]]])

(defcomponent Analysis [{{:keys [id switches]} :analysis switches-metadata :switches}]
  [:section {:key id}
   [:div.analysis-controls
    [:button {:on-click [:analyses/move-up id]} "up"]
    [:button {:on-click [:analyses/move-down id]} "down"]
    [:button {:on-click [:analyses/remove id]} "del"]
    " : "
    (interpose " | "
      (for [[index switch-name] (map-indexed vector switches)]
        [:strong {:style {:color (first (drop (* 2 index) (cycle charts/colors)))}}
         (utils/clean-switch-name switch-name)
         [:button {:on-click (if (= 1 (count switches))
                               [:analyses/remove id]
                               [:analyses/remove-switch switch-name id])}
          "x"]]))]
   (VegaLite (charts/force-curve-spec switches-metadata switches))])

(defcomponent Analyses [{{:keys [analyses] :as state} :state {:keys [sources switches]} :metadata}]
  [:main.analyses
   ; [:code (pr-str state)]
   (if (seq analyses)
     (for [analysis analyses]
       (Analysis {:analysis analysis
                  :switches switches}))
     [:div.main-message
      [:p "Powered by: "
       (for [{:keys [author url]} (vals sources)]
         [:a {:href url :target "_blank"} author])]])])

(defcomponent AddSwitchDialog [{{:keys [top switch]} :add-switch-dialog analyses :analyses}]
  [:div.add-switch-dialog
   {:style {:top (- top 10)}
    :on-mouse-leave [:switches/hide-add-dialog]}
   [:ul.dialog-list
    (concat
      (for [{[first-switch] :switches id :id} analyses]
        [:li.dialog-list-item
         [:button {:on-click [[:switches/hide-add-dialog]
                              [:analyses/add-switch switch id]]}
          (utils/clean-switch-name first-switch)]])
      (when (seq analyses)
        [[:li.dialog-list-divider [:hr]]])
      [[:li.dialog-list-item.dialog-list-item-new
        [:button {:on-click [[:switches/hide-add-dialog]
                             [:analyses/new switch]]}
         "new"]]])]])

(defcomponent App [{:keys [metadata state] :as input}]
  [:div.container
   (SidePanel input)
   (Analyses input)
   (when (:add-switch-dialog state)
     (AddSwitchDialog state))])
