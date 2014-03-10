(ns efreports.views.reports.reports-view
  (:use [hiccup.def]
        [hiccup.core]
        [hiccup.form :only (form-to check-box label text-area
                            text-field hidden-field submit-button)]
        [hiccup.page :only [include-js]])
  (:require [efreports.views.layout :as layout]
            [efreports.views.streams.streams-markup :as sm]
            [efreports.helpers.html-components :as hc]
            [clojure.string :as clj-string]
            [clj-time.core :as clj-time]
            [clj-time.coerce :as clj-time-coerce]
            [clj-time.format :as time-format])
  (:import  [java.lang String]))




(defn create-report
  ;;resonds to ajax report creation controller method
  ;;so don't render the layout - only the form html
  [report-params]
  (sm/save-report (report-params :stream) (report-params :report_name) (report-params :report_items_per)))



(defn report-header [report-params]

  (layout/common (report-params :report-name) (report-params :username)
    (concat
         (html
          [:div {:class "row"}
            [:div {:class "col-md-3"}[:h2 (report-params :report-name)]]
            [:div {:class "col-md-3 pull-right"}

             [:p "Last refreshed: "
               [:abbr
                {:class "timeago" :title
                 (time-format/unparse (time-format/formatters :date-time)
                                    (report-params :last-refresh))}]]
             [:a {:class "btn btn-danger" :id "report-refresh-button" :href (str "/reports/refresh/" (clj-string/replace
                                                                          (report-params :report-name) #" " "%20"))}  [:i {:class "icon-refresh"}] " Refresh"]

             ]

           ])
      (sm/report-table (report-params :report-name) "" "" "" "render" "" "" "report")
      (include-js "/javascript/stream-data.js"))))

(defn report-body
  [report-params rs per page rs-total column-map visible-count]
    (concat
         (html
          [:div {:class "row"}
                  [:div {:class "col-md-3"} [:h4 (str "Returned " rs-total " items")]] [:div {:class "col-md-3 pull-right"} (sm/download-formats-button
                                                                                       (str "/reports/view/" (report-params :report-name)))]]

          [:div {:class "row"}
                  [:div {:class "col-md-3"} [:h5 "Displaying " per " items per page"]]]

          )
         (sm/rs-to-report-table (report-params :report-name) ""
                                  rs "" column-map "" "" rs-total visible-count "report")
         (hc/pagination-bar (report-params :report-name) per page rs-total)))





