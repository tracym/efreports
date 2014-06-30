(ns efreports.helpers.data
    (:use [clojure.set])
		(:require [clojure.java.jdbc :as sql]
				  		[clojure-csv.core :as csv]
              [clojure.core.memoize :as memo])
    (:import com.mchange.v2.c3p0.ComboPooledDataSource))



(defn sort-column-map [colmap column-map-ordering]
  (if (not (empty? column-map-ordering))
    (->> column-map-ordering
         (map #(assoc {} (keyword (% :name)) (% :friendly-name)))
         (into {})
         (into (sorted-map-by (fn [lkey, rkey] (compare ((first (filter #(= (keyword (% :name)) lkey) column-map-ordering)) :order)
                                                       ((first (filter #(= (keyword (% :name)) rkey) column-map-ordering)) :order))))))
   (colmap)))


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

(def num-operator-map {"eq" = "ne" not= "gt" > "lt" < })

(defn filter-comparison
  "crude"
  [op-str lvalue rvalue]
  (if (and (number? lvalue) (number? rvalue))
    ((num-operator-map op-str) lvalue rvalue)
    (if (and (string? lvalue) (string? rvalue))
      (case op-str
        (or "eq" "ne") ((num-operator-map op-str) lvalue rvalue)
        "gt" (> (compare lvalue rvalue) 0)
        "lt" (< (compare lvalue rvalue) 0)))))


(defn filter-seq-by-filter-map
  "where seq-map is a sequence of maps and filter map is a map of filter criteria in this format:
   {:keyval {:some-key 'some val'} :operator 'eq'}"
  [seq-map filter-map]
  (filter #(filter-comparison (filter-map :operator)
            (% (first (keys (filter-map :keyval))))
            (first (vals (filter-map :keyval)))) seq-map))


(defn filter-seq-by-map-item
  "Filter a sequence of maps by a column and value (contained in a map).
   Note that we are only using equality for a comparison now."
   [seq-map keyval-map]
  (filter #(= (% (key keyval-map)) (val keyval-map)) seq-map))

(defn filter-seq-by-multiple-map-items
  "Filter a sequence of maps by a map of criteria"
  [seq-map filter-criteria]
  (reduce filter-seq-by-map-item seq-map filter-criteria))


(defn filter-seq-by-multiple-filter-maps
  "Filter a sequence of maps by a map of criteria"
  [seq-map filter-criteria]
  (reduce filter-seq-by-filter-map seq-map filter-criteria))


(defn get-empty-map [lmap rmap]
  (let [ks (seq (union (set (keys lmap)) (set (keys rmap))))
        n (count ks)]
      (zipmap ks (repeat n nil))))

(defn merge-map-with-empty [lmap rmap]
  (merge-with #(or %1 %2) lmap
    (get-empty-map lmap rmap)))

(defn left-join-multi-key [lmap rmap keycols]
  (let [keym (map (fn [o] (select-keys o keycols)) lmap)]
    (map (fn [n] (let [res (filter-seq-by-multiple-map-items rmap n)]
              (if (empty? res)
              (merge-map-with-empty (first (filter-seq-by-multiple-map-items lmap n)) (first rmap))
              (merge (first (filter-seq-by-multiple-map-items lmap n)) (first res))))) keym)))





(defn pivot-row
  ;; PRE: :total is the data we want to see in the pivot
  [x-key y-key row]
    (into (sorted-map) (mapcat #(assoc {y-key (% y-key)} (keyword (% x-key)) (% :total)) row)))


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
            existing-x-maps (filter-seq-by-multiple-map-items totalled-rs (key y-item))
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
