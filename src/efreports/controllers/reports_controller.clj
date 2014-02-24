(ns efreports.controllers.reports-controller
  (:use [compojure.core :only (defroutes GET POST)]
        [clojure.tools.trace])
  (:require [clojure.string :as clj-str]
            [ring.util.response :as ring]
            [efreports.models.reports-model :as report-model]
            [efreports.models.streams-model :as stream-model]
            [efreports.views.reports.reports-view :as report-view]
            [efreports.helpers.data :as data]
            [efreports.helpers.ring-plumbing :as plumbing]
            [efreports.helpers.stream-session :as stream-sess]
            [efreports.helpers.stream-manipulations :as stream-manip]
            [efreports.helpers.export.excel :as xl]
            ))

(def ^:const dflt-first-page 1)
(def ^:const dflt-per-page 10)


(defn create-or-update-report
  [report-params]

  (let [user (report-params :username)
        stream (report-params :stream)
        report-name (report-params :report_name) ;;hopefullly we can limit the use of underscores to this function. This because of the javascript/ajax
        stream-state (stream-sess/stream-attributes user stream)
        items-per (report-params :report_items_per)
        existing-report (report-model/find-report-map (report-params :report_name))]

        (if existing-report
          (when (report-model/update-report existing-report stream-state items-per)
            (report-view/create-report report-params))

          (when (report-model/create-report report-name user stream-state items-per)
            (report-view/create-report report-params)))))

(defn report-body
  [report-params]

  (let [user (report-params :username)
        report-name (report-params :report-name)

        report-obj (report-model/find-latest-report-map-by-report user report-name)
        stream-obj (stream-model/find-stream-map (report-obj :base-stream))

        base-rs (data/cached-query user (report-obj :last-refresh) (stream-obj :sql))

        manip-rs (stream-manip/apply-stored-manip (report-obj :stream-manipulations) base-rs)

        supplanted-manip-data (merge {:rs-keys (keys (first manip-rs))} (report-obj :stream-manipulations))

        manip-column-map-ordering (stream-manip/manip-column-map-ordering supplanted-manip-data (stream-obj :column-map))
        manip-column-map (stream-manip/manip-column-map supplanted-manip-data manip-column-map-ordering)


        total-results (count manip-rs)

        page (if (contains? report-params :page) (data/parse-int (report-params :page)) dflt-first-page)
        paginated-rs (stream-manip/paginate-stream-rs manip-rs
                                                      (data/parse-int (report-obj :items-per-page))
                                                      page)

        visible-columns ((report-obj :stream-manipulations) :column-display-count)


        ]

       (if (contains? report-params :format)

         (when (= (report-params :format) "xls")
           (let [wb (xl/seq-map->workbook report-name manip-rs)
                 wb-bytes (xl/workbook->ByteArryInputStream wb)]
             (ring/content-type (ring/response wb-bytes) "application/vnd.ms-excel")))

         (report-view/report-body report-params paginated-rs
                                (data/parse-int (report-obj :items-per-page))
                                page total-results manip-column-map visible-columns))


    )
  )


(defn report-header
  [report-params]
  (let [last-refresh ((report-model/find-report-map (report-params :report-name)) :last-refresh)
        dl-format (report-params :format)
        merged-params (merge report-params {:last-refresh last-refresh})]

    (if dl-format
      (report-body merged-params)
      (report-view/report-header merged-params))))





(defn refresh-report [report-params]
  (println "report refresh params" report-params)
  (let [report-obj (report-model/find-latest-report-map-by-report
                                                                  (report-params :username)
                                                                  (report-params :report-name))]
    (report-model/refresh-report report-obj)
    (report-body report-params)))


(defroutes routes
  (POST "/reports/create" {report-params :params session :session}
        (plumbing/wrap-session-and-route session report-params create-or-update-report))

  (POST "/reports/refresh/:report-name" {report-params :params session :session}
        (plumbing/wrap-session-and-route session report-params refresh-report))

  (GET "/reports/view/:report-name" {report-params :params session :session}
        (plumbing/wrap-session-and-route session report-params report-header))

  (GET "/reports/body/:report-name" {report-params :params session :session}
        (plumbing/wrap-session-and-route session report-params report-body))

  )











