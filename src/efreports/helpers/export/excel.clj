(ns efreports.helpers.export.excel
  (:require [clj-excel.core :as clj-xl]
            [clj-time.core :as clj-time]
            [clj-time.format :as time-format])

  )

(def ^:const dflt-tmp-excel-location "/home/tracy/")

(defn seq-map->workbook [stream seq-map]
 (let [wb-name (str stream "_" (time-format/unparse
                                   (time-format/formatter "yyyyMMdd_hhmmss")
                                   (clj-time/now)))]

  (clj-xl/build-workbook (clj-xl/workbook-hssf)
                        {wb-name (cons (into [] (map name (keys (first seq-map))))
                                      (for [m seq-map] (into [] (vals m))))})))


(defn workbook->ByteArryInputStream [workbook]
  (let [bstream (new java.io.ByteArrayOutputStream)
        byte-out (.write workbook bstream)]
    (new java.io.ByteArrayInputStream (.toByteArray bstream))))


(defn save-to-temp-location [wb stream]
  (let [filename (str stream "_"  (time-format/unparse
                                   (time-format/formatter "yyyyMMdd_hhmmss")
                                   (clj-time/now)) ".xlsx")]
    (clj-xl/save wb (str dflt-tmp-excel-location filename))
    (str dflt-tmp-excel-location filename)))

