(ns efreports.helpers.charts
  (require [cheshire.core :refer :all]
           [clojure.math.numeric-tower :as math])
  )

;; for chart.js
(def chart-color-maps [
                         {
                          :fillColor "rgba(199,106,48,0.5)"
                          :strokeColor "rgba(199,106,48,1)"
                          :pointColor "rgba(199,106,48,1)"
                          :pointStrokeColor "#fff"},

                         {
                          :fillColor "rgba(204,179,102,0.5)"
                          :strokeColor "rgba(204,179,102,1)"
                          :pointColor "rgba(204,179,102,1)"
                          :pointStrokeColor "#fff"
                         },

                         {
                          :fillColor "rgba(137,137,99,0.5)"
                          :strokeColor "rgba(137,137,99,1)"
                          :pointColor "rgba(137,137,99,1)"
                          :pointStrokeColor "#fff"
                         },

                         {
                          :fillColor "rgba(102,149,175,0.5)"
                          :strokeColor "rgba(102,149,175,1)"
                          :pointColor "rgba(102,149,175,1)"
                          :pointStrokeColor "#fff"
                         },

                         {
                          :fillColor "rgba(42,58,83,0.5)"
                          :strokeColor "rgba(42,58,83,1)"
                          :pointColor "rgba(42,58,83,1)"
                          :pointStrokeColor "#fff"
                         },

                         {
                          :fillColor "rgba(169,179,183,0.5)"
                          :strokeColor "rgba(169,179,183,1)"
                          :pointColor "rgba(169,179,183,1)"
                          :pointStrokeColor "#fff"
                         },

                         {
                          :fillColor "rgba(224,232,234,0.5)"
                          :strokeColor "rgba(224,232,234,1)"
                          :pointColor "rgba(224,232,234,1)"
                          :pointStrokeColor "#fff"
                         },

                         {
                          :fillColor "rgba(126,118,131,0.5)"
                          :strokeColor "rgba(126,118,131,1)"
                          :pointColor "rgba(126,118,131,1)"
                          :pointStrokeColor "#fff"
                         }])

(defn pivot-row->chart-dataset [row y-key]
  (let [row-data (->> row
                     (remove #(= (key %) y-key))
                     (vals)
                     (into []))]
    (assoc (nth chart-color-maps (math/ceil (rand 7))) :data row-data)))


;; (defn pivot-table->line-chart-json [pivot-table x-key y-key]
;;   (let [labels (->> (first pivot-table)
;;                     (keys)
;;                     (map name)
;;                     (remove #(= % (name y-key)))
;;                     (into []))
;;         datasets  (->> pivot-table
;;                        (map (fn [row] (pivot-row->chart-dataset row y-key)))
;;                        (into []))]

;;     (generate-string (assoc {:labels labels} :datasets datasets))))
;;</for chart.js>

(defn pivot-table->line-chart-json [pivot-table x-key y-key]
  (let [y-col (name (key (first (select-keys (first pivot-table) [y-key]))))
        x-cols (into [] (map name (remove #(= % y-key) (keys (first pivot-table)))))
        header-row (into [] (cons y-col x-cols))
        data-rows   (for [row pivot-table]
                      (into [] (map #(get row (keyword %)) header-row)))]
      (generate-string (cons header-row data-rows))))

(defn totalled-rs->line-chart-json [totalled-rs]
  (let [total-header (into [] (map name (remove #(= % :total) (keys (first totalled-rs)))))
        full-header (conj total-header "total")
        data-rows   (for [row totalled-rs]
                      (into [] (map #(get row (keyword %)) full-header)))]
       (generate-string (cons full-header data-rows))))
