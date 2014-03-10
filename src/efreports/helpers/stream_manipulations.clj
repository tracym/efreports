(ns efreports.helpers.stream-manipulations
  (:require [efreports.helpers.data :as data]
            [efreports.models.streams-model :as stream-model]
            [efreports.models.reports-model :as report-model]

            [clojure.set :as clj-set]
            [clojure.string :as clj-string])

  )


(defn nil-or-empty? [thing]
  (or (nil? thing) (empty? thing))
  )

(defn paginate-stream-rs [rs per page]
  (take-last per (take (* page per) rs)))

(defn total-rs [key-vec rs]
  (for [m (group-by #(select-keys % key-vec) rs)]
      (merge (first m) {:total (count (val m))})))

(defn sort-rs-from-map [sess-sort-map rs]
   (if-not (empty? sess-sort-map)
    (let [sm (into {} (for [m sess-sort-map] {(key m) (ns-resolve 'efreports.helpers.data (symbol (val m)))}))]
      (sort (apply data/compare-by (flatten (vec sm))) rs))
    rs))


(defn column-maps-for-streams [stream-list]
  (->> stream-list
        (map #((stream-model/find-stream-map %) :column-map))
        (reduce merge)))


(defn init-column-map-ordering
  "For each item in a column map assign it an order and visibility status"
  [column-map start-index]
  (map #(merge {:order %1} {:name (clj-string/replace (str (key %2)) #":" "")
                                          :friendly-name (val %2) }) (iterate inc start-index) column-map)
 )


(defn insert-column-into-column-map-ordering
  [column-map-ordering-instance column-map-ordering position]
  (let [cmo column-map-ordering
        cmo-inst column-map-ordering-instance]
    (when-not (and (= (- position 1) (cmo-inst :order))
                   (nil? cmo)
                   (empty? cmo))
      (let [col-index (first (filter #(= (% :name) (cmo-inst :name)) cmo))
            from-position (if (not col-index) (count cmo) (col-index :order))]

          (concat
               (cons
                 (merge (dissoc cmo-inst :order) {:order (- position 1)})
                 (filter #(<= (% :order) (- position 2)) cmo))
            (for [m (filter #(and (< (% :order) from-position)
                                  (>= (% :order) (- position 1)))
                                  (remove #(= (% :name) (cmo-inst :name)) cmo))] (update-in m [:order] inc))

            (filter #(> (% :order) from-position) cmo))))))

(defn arrange-total-columns [column-map-ordering total-cols]
    (let [total-position (+ (count total-cols) 1)
          total-col-strings (doall (map #(clj-string/replace (str %) #":" "") total-cols))
          col-instances (remove nil? (doall (map (fn [x] (first (filter #(= (% :name) x) column-map-ordering))) total-col-strings)))]

        (let [new-cmo (reduce #(insert-column-into-column-map-ordering %2 %1 1)
                                column-map-ordering col-instances)]



            (let [cmo-with-total (if (empty? (filter #(= (% :name) "total") new-cmo))
                                                   (insert-column-into-column-map-ordering
                                                       {:name "total" :friendly-name "Total" :order total-position}
                                                       new-cmo
                                                       total-position)
                                      new-cmo)]

              cmo-with-total
              )))) ;;insert the totals




(defn concat-column-map-ordering
  [column-map-ordering mapped-cols]
    (let [catted-cmo (when-not (and (empty? mapped-cols) (empty? column-map-ordering))
                      (let [to-append (init-column-map-ordering mapped-cols (+ (count column-map-ordering) 1))]
                         (concat column-map-ordering to-append)))]
      catted-cmo))


(defn generate-pivot-column-map-ordering [column-map y-key]
  (let [init-colmap (init-column-map-ordering column-map 0)]
    (insert-column-into-column-map-ordering  (first (filter #(= (% :name) (name y-key)) init-colmap))
                                                     init-colmap 1)))


(defn manip-column-map
  [manip-data column-map-ordering]
   (let [streams-to-map (manip-data :mapped-streams)
         pivot-totals (manip-data :pivot-totals)
         rs-keys (manip-data :rs-keys)
         stream (manip-data :name)
         total-columns (manip-data :total-cols)
         base-column-map ((stream-model/find-stream-map stream) :column-map)
         ]

        (if pivot-totals
          (let [base-pivot-column-map (into {} (mapcat #(assoc {} % (name %)) rs-keys))]
              (data/sort-column-map base-pivot-column-map column-map-ordering))

          (if streams-to-map
            (let [mapped-cols (column-maps-for-streams streams-to-map)]
              (data/sort-column-map (concat base-column-map mapped-cols) column-map-ordering))
            (data/sort-column-map base-column-map column-map-ordering)


            ))))


(defn manip-column-map-ordering
  "Based on a set of manipulations return a column map ordering"
  [manip-data base-column-map]
    (let [streams-to-map (manip-data :mapped-streams)
        filter-map (manip-data :filter-map)
        total-cols (seq (manip-data :total-cols))
        sort-map (manip-data :sort-map)
        supplied-cmo (seq (manip-data :column-map-ordering))
        user (manip-data :user)
        stream (manip-data :name)
        pivot-totals (manip-data :pivot-totals)
        rs-keys (manip-data :rs-keys)
        pivot-column-map (if rs-keys (into {} (mapcat #(assoc {} % (name %)) rs-keys)) base-column-map)
         ]

     (let [effective-cmo (if (not (nil-or-empty? pivot-totals))

                             (if supplied-cmo
                               (let [existing-keys (set (map #(keyword (% :name)) supplied-cmo))
                                     diff (clj-set/difference existing-keys (set rs-keys))
                                     ;;debug (println "existing keys " existing-keys)
                                     ;;debug1 (println "rs keys " rs-keys)
                                     ;;debug2 (println "diff keys " diff)
                                     ]
                                     (if (empty? diff)
                                       supplied-cmo
                                       (generate-pivot-column-map-ordering pivot-column-map (pivot-totals :y-key))))
                               (generate-pivot-column-map-ordering pivot-column-map (pivot-totals :y-key)))

                           (if (nil-or-empty? supplied-cmo)
                                 (init-column-map-ordering base-column-map 0)
                                 supplied-cmo))]



       (let [post-total-cmo (if (not (nil-or-empty? total-cols))
                                (doall (arrange-total-columns effective-cmo total-cols))
                                 effective-cmo)]

         (if (not (nil-or-empty? streams-to-map))
               (let [stream-cols ((stream-model/find-stream-map stream) :column-map)
                     mapped-cols (column-maps-for-streams streams-to-map)
                     total-col-count (+ (count stream-cols) (count mapped-cols))
                     base-col-count (count post-total-cmo)]

                    (if (or (= base-col-count total-col-count)
                            (= base-col-count (+ total-col-count 1))) ;; don't concatentate mapped column maps if we already have. Also check for total column
                         post-total-cmo
                         (concat-column-map-ordering post-total-cmo mapped-cols)))
                post-total-cmo)))
     )


  )


(defn apply-stored-manip
  "Sequentially apply set of stored operations to an unadorned result set from a datasource (usually a db)"
  [manip-data base-rs]
  (let [streams-to-map (manip-data :mapped-streams)
        stream (manip-data :name)
        filter-map (manip-data :filter-map)
        total-cols (vec (map keyword (manip-data :total-cols)))
        sort-map (manip-data :sort-map)
        pivot-totals (manip-data :pivot-totals)
        ]
    (if (not (nil-or-empty? pivot-totals))
      (let [pivot-x (keyword (pivot-totals :x-key))
            pivot-y (keyword (pivot-totals :y-key))
            totalled-rs (total-rs [pivot-x, pivot-y] base-rs)]
           (data/pivot-table pivot-x pivot-y totalled-rs)
      )

      (let [mrs (if (not (nil-or-empty? streams-to-map))
                    (let [join-keys (->> streams-to-map
                                     (cons stream)
                                     (map #(stream-model/find-stream-key-cols %))
                                     (map set)
                                     (reduce clj-set/intersection)
                                     (map keyword)
                                     (into []))

                          rs-seq  (->> streams-to-map
                                     (map #(stream-model/find-map-exec-sql (manip-data :user) (manip-data :last-refresh) %))
                                     (cons base-rs))]

                      (reduce #(data/left-join-multi-key %1 %2 join-keys) rs-seq)
                    )
                base-rs)]

        (let [frs (if (not (nil-or-empty? filter-map))
                    (data/filter-seq-by-multiple-filter-maps mrs filter-map)
                    mrs)]

          (let [trs  (if (not (nil-or-empty? total-cols))
                      ;;(println "and here " total-cols)
                       (total-rs total-cols frs)
                      frs)]


            (let [srs (if (not (nil-or-empty? sort-map))
                          (if (clj-set/subset? (set (keys sort-map)) (set (keys (first trs))))
                            (sort-rs-from-map sort-map trs)
                            trs) trs)]
              srs)


            ))))))












