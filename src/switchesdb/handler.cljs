(ns switchesdb.handler)

(defmulti handle (fn [_state [action-name & _action-args]] action-name))

(defmethod handle :analyses/add-switch [state [_ switch-name id]]
  (update state :analyses (fn [analyses]
                            (mapv (fn [analysis]
                                    (cond-> analysis
                                      (and (= id (:id analysis))
                                           (not (contains? (set (:switches analysis)) switch-name)))
                                      (update :switches conj switch-name)))
                                  analyses))))

(defmethod handle :analyses/remove-switch [state [_ switch-name id]]
  (update state :analyses (fn [analyses]
                            (mapv (fn [analysis]
                                    (cond-> analysis
                                      (= id (:id analysis))
                                      (update :switches (partial filterv #(not= switch-name %)))))
                                  analyses))))

(defmethod handle :analyses/new [state [_ switch-name]]
  (update state :analyses conj {:id (gensym)
                                :switches [switch-name]}))

(defn find-analysis-index [analyses id]
  (some (fn [[i analysis]]
          (when (= id (:id analysis)) i))
        (map-indexed vector analyses)))

(defn vec-swap-indices [v i1 i2]
  (if (= i1 i2)
    v
    (let [e1 (nth v i1)
          e2 (nth v i2)]
      (-> v
          (assoc i1 e2)
          (assoc i2 e1)))))

(defmethod handle :analyses/move-up [{:keys [analyses] :as state} [_ id]]
  (let [index (find-analysis-index analyses id)]
    (if (zero? index)
      state
      (update state :analyses vec-swap-indices index (dec index)))))

(defmethod handle :analyses/move-down [{:keys [analyses] :as state} [_ id]]
  (let [index (find-analysis-index analyses id)]
    (if (= index (dec (count analyses)))
      state
      (update state :analyses vec-swap-indices index (inc index)))))

(defmethod handle :analyses/remove [state [_ id]]
  (update state :analyses (fn [analyses]
                            (filterv #(not= id (:id %)) analyses))))
