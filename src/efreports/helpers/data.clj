(ns efreports.helpers.data
    (:use [clojure.set])
		(:require [clojure.java.jdbc :as sql]
				  		[clojure-csv.core :as csv]
              [clojure.core.memoize :as memo])
    (:import com.mchange.v2.c3p0.ComboPooledDataSource))


(defn sort-column-map [colmap column-map-ordering]
  (if (not (empty? column-map-ordering))
    (into (sorted-map-by (fn [lkey, rkey] (compare ((first (filter #(= (keyword (% :name)) lkey) column-map-ordering)) :order)
                                                 ((first (filter #(= (keyword (% :name)) rkey) column-map-ordering)) :order))))
      (into {} (map #(assoc {} (keyword (% :name)) (% :friendly-name)) column-map-ordering)))

  (colmap)))

;; (defn sorted-row-vals [rs-map column-map-ordering]
;;   ;;(into (sorted-map) (sort-by key compare rs-map))
;;   (if (not (empty? column-map-ordering))
;;     (into {} (mapcat #(assoc {} (keyword (% :name)) (% :friendly-name)) (sort-by :order column-map-ordering)))
;;     rs-map)
;;   )

(defn sorted-row-vals [rs-map column-map-ordering]
  (into (sorted-map) (sort-by key compare rs-map)))

(defn keys-to-strings [key-list]
  (map name key-list))

;; (defn column-headers [rs]
;;   (keys-to-strings (keys (first rs))))

;; (defn rs-column-headers [rs colmap]
;;   (vals (vals (zipmap (keys (first rs)) (sort-column-map colmap)))))

;; (defn rs-column-keys [rs colmap]
;;   (map name (keys (vals (zipmap (keys (first rs)) (sort-column-map colmap))))))


(defn columns [rs colmap]
  (let [rs-keys (keys (first rs))]
    (let [cols (select-keys colmap (keys (first rs)))]
     (if (some #(when (= % :total) %) rs-keys)
      (merge cols {:total "total"})
      cols))))


;;The code below to generate comparators for mulitple keys in a collection is taken from:
;;https://groups.google.com/d/msg/clojure/VVVa3TS15pU/pT3iG_W2VroJ
;;to use: (sort (compare-by :last-name asc, :date-of-birth desc) coll)
(def asc compare)
(def desc #(compare %2 %1))
;;  compare-by generates a Comparator:
(defn compare-by [& key-cmp-pairs]
  (fn [x y]
      (loop [[k cmp & more] key-cmp-pairs]
         (let [result (cmp (k x) (k y))]
              (if (and (zero? result) more)
                  (recur more)
                  result)))))

(defmacro generate-comp-from-map
  ;;Generates a comparator using compare-by and sort-map (which is a map of keys to asc or desc comparisons)
  [sort-map]
  `(apply compare-by (flatten (vec ~sort-map))))


(def db
  {:classname "org.postgresql.Driver"
  :subprotocol "postgresql"
  :subname "//localhost:5432/shakespeare"
  :user "shakespeare"
  :password "password"})




(defn pool
  [spec]
  (let [cpds (doto (ComboPooledDataSource.)
               (.setDriverClass (:classname spec))
               (.setJdbcUrl (str "jdbc:" (:subprotocol spec) ":" (:subname spec)))
               (.setUser (:user spec))
               (.setPassword (:password spec))
               ;; expire excess connections after 30 minutes of inactivity:
               (.setMaxIdleTimeExcessConnections (* 30 60))
               ;; expire connections after 3 hours of inactivity:
               (.setMaxIdleTime (* 3 60 60)))]
    {:datasource cpds}))


(def pooled-db (delay (pool db)))

(defn db-connection [] @pooled-db)

(defn exec-sql [sql-stmt & bind-params]
  (sql/with-connection (or (System/getenv "DATABASE_URL") (db-connection))
    (sql/with-query-results rs (vec (cons sql-stmt bind-params))
      (doall rs))))

(defn cached-query
  "Memoized call to exec-sql that tracks the user name and last refresh time.
   a change in either variable triggers a fresh db query."
  [user refresh-time sql-stmt]
      (exec-sql sql-stmt))

(def cached-query-memo (memo/fifo cached-query {} :fifo/threshold 50))

(defn open-csv [csv-filename]
  	(csv/parse-csv (slurp csv-filename)))


(defn csv-header-keys [csv-seq]
		(vec (map keyword (merge (first csv-seq) "id")))
)


(defn csv-data [csv-seq]
		(vec (rest csv-seq))
)

;; pass the full path to a csv and a get map of column headings to row values
(defn csv-to-map [csv-seq]
	(map (fn [row] (zipmap (csv-header-keys csv-seq) row))
										(map #(merge %1 %2) (csv-data csv-seq) (iterate inc 0)))
)

(defn csv-seq-to-csv [csv-seq]
  (concat (csv/write-csv [(map name (keys (first csv-seq)))])
          (csv/write-csv (for [m csv-seq] (vec (map str (vals m))))
            )))

(defn diff-by-key [lseq-map rseq-map keycol]
  (difference (project (set lseq-map) [keycol]) (project (set rseq-map) [keycol])))

; (defn left-join [lseq-map rseq-map keycol]

;   (join lseq-map rseq-map [keycol])

; )



(defn filter-seq-by-map
  "Filter a sequence of maps by a column and value (contained in a map).
   Note that we are only using equality for a comparison now."
   [seq-map keyval-map]
  (filter #(= (% (key keyval-map)) (val keyval-map)) seq-map))

(defn filter-seq-by-multiple
  "Filter a sequence of maps by a map of criteria"
  [seq-map filter-criteria]
  (reduce filter-seq-by-map seq-map filter-criteria))


(defn get-empty-map [lmap rmap]
  (let [ks (seq (union (set (keys lmap)) (set (keys rmap))))
        n (count ks)]
      (zipmap ks (repeat n nil))))

(defn merge-map-with-empty [lmap rmap]
  (merge-with #(or %1 %2) lmap
    (get-empty-map lmap rmap)))


(defn map-difference [m1 m2]
  (let [ks1 (set (keys m1))
        ks2 (set (keys m2))
        ks1-ks2 (difference ks1 ks2)
        ks2-ks1 (difference ks2 ks1)
        ks1*ks2 (intersection ks1 ks2)]
    (merge (select-keys m1 ks1-ks2)
           (select-keys m2 ks2-ks1)
           (select-keys m1
                        (remove (fn [k] (= (m1 k) (m2 k)))
                                ks1*ks2)))))

(defn pmerge-keyed-map-seq [lmap rmap keycol]
  (pmap
    (fn [m] (let [res (filter #(= (% keycol) (m keycol)) rmap)]
             (if (empty? res)
              (merge-map-with-empty m (first rmap))
              (merge m (first res))))) lmap))

(defn merge-keyed-map-seq [lmap rmap keycol]
  (for [m lmap]
    (let [res (filter #(= (% keycol) (m keycol)) rmap)]
      (if (empty? res)
        (merge-map-with-empty m (first rmap))
        (map-difference m (first res))))))


(defn left-join-multi-key [lmap rmap keycols]
  (let [keym (pmap (fn [o] (select-keys o keycols)) lmap)]
    (pmap (fn [n] (let [res (filter-seq-by-multiple rmap n)]
              (if (empty? res)
              (merge-map-with-empty (first (filter-seq-by-multiple lmap n)) (first rmap))
              (merge (first (filter-seq-by-multiple lmap n)) (first res))))) keym)))





(defn pivot-row
  ;; PRE: :total is the data we want to see in the pivot
  [x-key y-key row]
    (into (sorted-map) (mapcat #(assoc {y-key (% y-key)} (keyword (% x-key)) (% :total)) row)))


;; (defn pivot-table
;;   [x-key y-key totalled-rs]
;;   (let [y-index (index totalled-rs [y-key])
;;        distinct-x-vals (map #((first %) x-key)
;;                              (index totalled-rs [x-key]))

;;        total-groups (map #(filter-seq-by-multiple totalled-rs %)
;;                             (project totalled-rs [x-key]))]

;;       (map #(pivot-row x-key y-key %) total-groups)))

(defn pivot-table
  [x-key y-key totalled-rs]
  (let [y-index (index totalled-rs [y-key])

        distinct-x-vals (pmap #((first %) x-key)
                              (index totalled-rs [x-key]))
        ]

    (for [y-item y-index]
      (let [x-vals-per-y-key (pmap #(% x-key) (val y-item))

            missing-x-vals (seq (difference (set distinct-x-vals) (set x-vals-per-y-key)))
            missing-x-maps (for [missing missing-x-vals]
                             (merge {x-key missing :total 0} (key y-item)))
            existing-x-maps (filter-seq-by-multiple totalled-rs (key y-item))
            combined-maps (concat existing-x-maps missing-x-maps)]
           (pivot-row x-key y-key combined-maps)))))



;; Integer from a string
(defn parse-int [s]
  (Integer. (re-find  #"\d+" s )))

;; Ruby string interpolation
;; courtesy of  https://gist.github.com/blacktaxi/1676575
(defmacro fmt [^String string]
  (let [-re #"#\{(.*?)\}"
        fstr (clojure.string/replace string -re "%s")
        fargs (map #(read-string (second %)) (re-seq -re string))]
    `(format ~fstr ~@fargs)))
