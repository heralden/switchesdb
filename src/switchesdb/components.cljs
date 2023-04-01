(ns switchesdb.components
  {:clj-kondo/config '{:lint-as {dumdom.core/defcomponent clojure.core/defn}}}
  (:require [dumdom.core :refer [defcomponent]]
            [clojure.string :as str]
            [switchesdb.charts :as charts]
            [switchesdb.utils :as utils]
            [switchesdb.shared :refer [postfixes]]))

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
       (for [[switch-name switch-details] switches]
         [:li.switches-list-item
          [:button {:on-click [:analyses/new switch-name]} "+"]
          [:span.switches-list-name
           {:on-click [:switches/add-dialog switch-name]}
           (utils/clean-switch-name switch-name)
           [:code.source-badge (-> switch-details :source postfixes)]]])
       "No results")]))

(defcomponent FilterBox [{:keys [text]}]
  [:div.filter-box
   [:input {:type "text"
            :placeholder "Filter switches"
            :autofocus true
            :on-input [:filters/set-text]
            :value text}]])

(defcomponent SidePanel [{:keys [metadata state]}]
  [:aside.side-panel
   {:class (when-not (:mobile-side-panel? state) :hidden)
    :on-click [:switches/hide-add-dialog]}
   [:h1.side-title "SwitchesDB"
    (if (:mobile-side-panel? state)
      [:button.hide-side-panel
       {:on-click [:side-panel/toggle]} "«"]
      [:button.hide-side-panel.collapsed
       {:on-click [:side-panel/toggle]} "»"])]
   (FilterBox (:filters state))
   (SwitchesList {:switches (:switches metadata)
                  :filters (:filters state)})
   [:footer.side-footer
    [:a {:href "https://github.com/heralden/switchesdb" :target "_blank"}
     "Source"]]])

(defcomponent Analysis [{{:keys [id switches]} :analysis metadata :metadata}]
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
         [:code.source-badge (-> metadata :switches (get switch-name) :source postfixes)]
         [:button {:on-click (if (= 1 (count switches))
                               [:analyses/remove id]
                               [:analyses/remove-switch switch-name id])}
          "x"]]))]
   (VegaLite (charts/force-curve-spec metadata switches))])

(defcomponent Analyses [{{:keys [analyses] :as state} :state {:keys [sources] :as metadata} :metadata}]
  [:main.analyses
   {:on-click [[:switches/hide-add-dialog]
               [:side-panel/hide]]}
   ; [:code (pr-str state)]
   (if (seq analyses)
     (for [analysis analyses]
       (Analysis {:analysis analysis
                  :metadata metadata}))
     [:div.main-message
      [:p "Powered by: "
       (for [{:keys [author url]} (vals sources)]
         [:a.source-link {:href url :target "_blank"} author])]
      [:ul
       [:li "Add a switch from the left panel to analyse it"]
       [:li "Click a switch name to open a dialog for adding it to existing analyses"]]])])

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
