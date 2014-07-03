(ns efreports.views.streams.streams-markup
  (:require [clojure.string :as clj-string]
            [monger.joda-time]
            [monger.json]
            [clj-time.core :as clj-time]
            [clj-time.coerce :as clj-time-coerce]
            [clj-time.format :as time-format]
            [clojure.set :as clj-set]
            [ring.util.codec :as ring-codec]
            )
  (:use [hiccup.def]
        [hiccup.core]
        [hiccup.form :only (form-to check-box label text-area text-field hidden-field submit-button reset-button drop-down)]
        [efreports.helpers.html-components]
        [efreports.helpers.tag]
        [efreports.helpers.stream-session :only [column-map-ordering]]
        [efreports.helpers.data :only [sort-column-map]])
)

(defhtml filter-column-cell [stream-name filter-col filter-val]
  [:td {:class filter-col}
   (form-to {:name "filter-column-cell-form" :class "form-horizontal"}
            [:get (str "/streams/data-header/"  (ring-codec/url-encode stream-name))]
      (hidden-field "fn" "filter-map-add")
      (hidden-field (str "filter-key-" filter-col) filter-val)

      (hidden-field "filter-op-container" "")
       ;;(submit-button {:class "btn btn-link filter-col-cell"} filter-val)
       (html [:div {:class "btn-group"}
             [:button {:class "btn btn-default dropdown-toggle" :data-toggle "dropdown"}
              (str filter-val "&nbsp;")
              [:span {:class "caret"}]]
             [:ul {:class "dropdown-menu" :role "menu"}
              [:li {:class "filter-op" :data-op "eq"} [:a {:href "#"} "Equals"]]
              [:li {:class "filter-op" :data-op "gt"} [:a {:href "#"} "Greater Than"]]
              [:li {:class "filter-op" :data-op "lt"} [:a {:href "#"} "Less Than"]]
              [:li {:class "filter-op" :data-op "ne"} [:a {:href "#"} "Does Not Equal"]]
              ]])

      )])

(def operator-display-map {"eq" " is " "ne" " is not " "lt" " is less than " "gt" " is greater than "})

(defhtml filter-pills [filter-map colmap]
  (when (not (empty? filter-map))

    (html [:div {:class "row"}
       [:div {:class "col-md-12"} [:h4 "Filtered by:"]]
        [:div {:class "row"}
        ;;[:div {:class "col-md-9"}
          [:ul {:class "filters"}

             (for [m filter-map]

               [:li {:class "filter-pill well well-small col-xs-3" :id (first (keys (m :keyval)))
                     :data-filter-val (first (vals (m :keyval))) :data-filter-op (m :operator)}
                 (str (colmap (first (keys (m :keyval)))) (operator-display-map (m :operator))) (html [:strong (first (vals (m :keyval)))]) [:a {:class "close"} "&times;"]])]]]))
  )

(defhtml download-formats-button [stream-url]
  (html [:div {:class "btn-group"}
         [:a {:class "btn btn-success dropdown-toggle"
              :data-toggle "dropdown" :href "#"}
              [:i {:class "icon-download-alt"}] " Download All Items "
              [:span {:class "caret"}]
         ]
         [:ul {:class "dropdown-menu"}
          [:li [:a {:href (str stream-url "?format=xls")}
                [:i {:class "icon-table"}] " Excel (.xslx)" ]]]]))

(defn map-view-tds [col-vals col-names filter-keys stream-name url-params]
  (map (fn [x, c]
    (if (some #(= c %) (map name filter-keys))
      (filter-column-cell stream-name c x)
      (html [:td {:class c} x])))
  col-vals col-names))

(defhtml report-table [stream-name to-stream-name thead-content
                       tbody-content manip-fn total-results
                       visible-col-count report-type]
  [:div {:data-stream stream-name :data-to-stream to-stream-name :data-manip-fn manip-fn :id stream-name :data-report-type report-type}
    [:table {:class "table table-hover table-condensed table-responsive"
             :data-total-results total-results
             :data-stream stream-name
             :data-visible-columns visible-col-count

             }
      (concat thead-content tbody-content)]
  ])


(defhtml report-thead [rs column-map report-type]

  [:thead  (vec (conj [:tr]
                (for [x (vec (map-tag-class :th (vals column-map)
                                                (keys column-map)))]
                        (into x (map-tag-class :a ["&times;"]["close"])))
                      (if (not (= report-type "report"))
                        [:th {:class "add-button"} (bootstrap-dd-button "Add Data")]
                        [:th])))]
)

(defhtml report-tbody [stream-name rs column-map filter-keys filter-query-prefix]
  [:tbody (map (fn [row &]
                  ;;(vec
                   (conj [:tr] (map-view-tds (for [col column-map] (get row (key col)))
                                             (map name (keys column-map))
                                                 filter-keys stream-name filter-query-prefix))) rs)])



(defhtml rs-to-report-table
    [stream-name to-stream-name rs filter-keys
     column-map manip-fn filter-query-prefix total-results
     visible-col-count report-type]
  (report-table stream-name to-stream-name (report-thead rs column-map report-type)
                                           (report-tbody stream-name rs column-map filter-keys filter-query-prefix)
                                            manip-fn total-results visible-col-count report-type))


(defhtml stream-display [{:keys [stream-name description]}]
   [:div {:class "col-md-3 list-group"}
    [:h4 {:class "list-group-item-heading"}
         [:a  {:class "" :href (str "/streams/data-header/" stream-name)} stream-name]]
    [:p {:class "list-group-item-text"} description]
    [:div {:class "btn-group"}
      [:a {:class "btn btn-default"
          :href (str "streams/edit/" stream-name)} "Edit "]
      [:a  {:class "btn btn-primary" :rel "nofollow"
            :href (str "streams/delete/" stream-name)} "Delete "]]])

(defhtml report-display [{:keys [report-name base-stream date-created]}]
   [:div {:class "col-md-3 list-group"}
    [:h4 {:class "list-group-item-heading"}
     [:a  {:class "" :href (str "/reports/view/" report-name)} report-name]]
    [:p {:class "list-group-item-text"}(str "Created using " base-stream)]
    [:abbr {:class "timeago" :title date-created}]
    ;;[:div {:class "btn-group"}]

    ])

(defhtml streams-container [streams]
    (for [m (partition 4 4 [] streams)]
      (html (map stream-display m)))
    [:div {:class "clearfix"}]
    [:a {:class "btn btn-success"
                  :href (str "/streams/new")} "Create New Collection"])


(defhtml reports-container [reports]
    (for [m (partition 4 4 [] reports)]
      (html (map report-display m)
            [:div {:class "clearfix"}]
            ))
    [:div {:class "clearfix"}])



(defhtml render-inline-checkbox [value data checked & icons]
    (label  {:class "col-sm-2 control-label" :for data} data value)
    (html [:div {:class "col-sm-2"}
      (let [icon-on (if (not (empty? icons)) (first icons)(str "<i class='icon-ok'></i>"))
            icon-off (if (not (empty? icons)) (second icons)(str "<i class='icon-check-empty'></i>"))]
        (html [:div {:class "make-switch switch-small" :data-on "success" :data-on-label icon-on :data-off-label icon-off }

          (check-box {:id data :class "form-control col-sm-2"
                  (keyword (first (clj-string/split checked #"="))) (last (clj-string/split checked #"="))} ;;all this for checked='checked'
               data)]))]))


(defhtml render-column-map-checks [colmap col-count checked-cols]
  (for [m (partition col-count col-count [] colmap)]
     [:div {:class "form-group"}
     (for [n m]
        (render-inline-checkbox (val n) (key n)
                                (if (empty? (filter #(= (key n) %) checked-cols))
                                  (str "")
                                  (str "checked='checked'"))))] [:hr])
         [:hr])


(defhtml column-map-entry [m keycols]

  (label {:class "col-xs-2 control-label"}  (name (key m)) (name (key m)))
  [:div {:class "col-xs-2"}
    (text-field {:placeholder (name (key m)) :class "form-control"} (name (key m)) (val m))]
  [:div {:class "col-xs-2"}
   (html [:div {:class "make-switch" :data-on-label "<i class='icon-ok icon-key'></i>"
                :data-off-label "<i class='icon-list-alt'></i>" }
          (let [cb-id (str "key_col_ind_" (clj-string/replace (key m) #":" ""))]
           (check-box (merge {:id cb-id :name cb-id :class "form-control"}
                             (if (some #(= % (key m)) (map keyword keycols))
                                  {:checked "'checked'"}
                                  nil)) (val m)))])])

(defhtml render-column-map-fields [colmap col-count keycols]
  (for [m (partition col-count col-count [] colmap)]
          (html [:div {:class "form-group"}
              (for [n m]
                  (column-map-entry n keycols))])))

(defhtml column-map-form [column-map stream-name keycols]
  (form-to {:name "column-map-data" :class "form-horizontal"}
    [:post "/streams/update/column-map"]

      [:legend "Column Names"]
      (hidden-field "stream" stream-name)
      (render-column-map-fields column-map 2 keycols)
       [:hr]
          (html [:a {:class "btn btn-default" :href "/"} "&laquo;&nbsp;Back to Index"] "&nbsp;")
          (submit-button {:class "btn btn-primary"} "Update Column Info")))


(defhtml stream-form [{:keys [stream-name description sql column-map action]}]
  [:div {:id "stream-form"}
    (form-to {:name "stream-data" :role "form" :class "form-horizontal"} [:post (str "/streams/" action)]
      [:div {:class "form-group"}
        [:legend "Basic Information"]

          (boostrap-form-control "Name" "stream-name" stream-name text-field)
          (boostrap-form-control "Brief Descrpition" "description" description text-field)

          [:div {:class "form-group"}
            (label {:class "control-label col-md-2"} "sql" "Enter the sql for your stream")
            [:div {:class "col-md-10"}
              (text-area {:rows "15" :class "form-control"} "sql" sql)]
          ]
      ]
      ;(render-colmap-checkboxes column-map)
      (html [:a {:class "btn btn-default" :href "/"} "&laquo;&nbsp;Back to Index"] "&nbsp;")
      (submit-button {:class "btn btn-primary"} (str (clj-string/capitalize action) " Collection")))

    ])


(defhtml totals-form [column-map stream-name total-cols]

  (form-to {:name "column-total-data" :class "form-horizontal" :role "form"}
           [:get (str "/streams/data-header/" (ring-codec/url-encode stream-name))]
           ;;need to add stream name here. was removed to keep parser from bitching

      [:h4 "Total Columns"]
      [:p "Group and total data by the columns selected here"]
      [:hr]
      (hidden-field "fn" "total-stream")
      (hidden-field "stream" stream-name)
      (render-column-map-checks column-map 3 total-cols)

          (submit-button {:class "btn btn-primary" :id "total-stream-button"} "Total"))

  [:hr])

(defhtml filtercols-form [column-map stream-name filter-cols]
  (form-to {:name "column-filter-data" :class "form-horizontal" :role "form"}
           [:get (str "/streams/data-header/" (ring-codec/url-encode stream-name))] ;;need to add stream name here. was removed to keep parser from bitching

      [:h4 "Filter Columns"]
      [:p "Columns selected here will act as clickable filters in the pane below"]
      [:hr]
      (hidden-field "fn" "filter-cols")
      (hidden-field "stream" stream-name)
      (render-column-map-checks column-map 3 filter-cols)

          (submit-button {:class "btn btn-primary" :id "filter-stream-button"} "Create Filters"))[:hr])

(defhtml relate-stream-form [{:keys [from-stream-name stream-name description mapped-streams]}]
  ;;need to make this so it supports unmapping based on whether or not form is already mapped
  [:div {:class "col-md-3"}
   (prn mapped-streams)
   (form-to {:name from-stream-name :class "form-horizontal"} [:get (str "/streams/data-header/" (clj-string/replace from-stream-name #" " "%20"))] ;;need to add stream name here. was removed to keep parser from bitching
      [:h4 stream-name]
      [:p description]
      (hidden-field "from-stream"  (ring-codec/url-encode from-stream-name))
      (hidden-field "to-stream" (ring-codec/url-encode stream-name))
      ;;(hidden-field "fn" (str description "-stream"))
      (let [action  (if (some #(= % stream-name)
                              mapped-streams)(str "unmap")(str "map"))
            button-text (if (= action "map")(str "Map")(str "Unmap"))]
          (concat
            (html (hidden-field "fn" (str action "-stream")))
            (html (submit-button {:class (str "btn " (if (= action "map")
                                                 (str "btn-primary")
                                                 (str "btn-warning")))
                                  :id (str action "-stream-" (ring-codec/url-encode stream-name))
                                  }
                           (str button-text " Collection"))))))
  ])

(defhtml related-streams [column-map stream-name rel-streams mapped-streams]
  (html
   [:h4 "Related Collections"]
   [:p "Map a stream to use data from it"]
   [:hr]
   [:div {:class "container-fluid"}
         (when (empty? rel-streams) (html [:p "No Related Collections"][:hr]))
    (for [m (partition 4 4 [] rel-streams)]
        [:div {:class "row-fluid"}
          (map relate-stream-form (for [n m] (merge n {:from-stream-name stream-name :mapped-streams mapped-streams})))]
        )][:div {:class "clearfix"}][:hr]))

(defhtml refresh-stream [stream-name last-refresh]
  [:hr]
  [:p "Last refreshed: "[:abbr {:class "timeago" :title
                                (time-format/unparse (time-format/formatters :date-time)
                                                     (clj-time-coerce/from-date last-refresh))}]]
  (form-to {:name stream-name :class "form-horizontal" :role "form"}
           [:get (str "/streams/data-header/" (ring-codec/url-encode stream-name))]
           
    (hidden-field "fn" "refresh")
    (hidden-field "refresh-datetime" (time-format/unparse (time-format/formatters :date-time)
                                                          (clj-time-coerce/from-string last-refresh)))
    (submit-button {:class "btn btn-danger"}  "Refresh"))
  [:hr]
  )

(defhtml save-report [stream-name report-name items-per]
  [:hr]
  (form-to {:name stream-name :class "form-horizontal" :role "form"} [:POST (str "/reports/new")]
    (hidden-field "fn" "save-report")
    (hidden-field "stream" stream-name)

    [:div {:class "form-group"}
    (label {:class "control-label col-lg-2"} "report-name" "Report Name")
    [:div {:class "col-lg-6"}
     (text-field (if report-name {:class "form-control" :disabled ""} {:class "form-control"})
                 "report-name" (if report-name report-name "Enter a name"))]
    ]

    (html [:div {:class "form-group"}
      (label {:class "control-label col-lg-2"} "report-items-per" "Items Per Page")
              [:div {:class "col-lg-10"}
              (text-field {:class "form-control slider"
                           :data-slider-min "0" :data-slider-max "1000"
                           :data-slider-step "10" :data-slider-value  (if items-per items-per "10")
                           :data-slider-orientation "horizontal"} "report-items-per" (if items-per items-per "10"))]
          ])


    (submit-button {:class "btn btn-primary" :id "save-report"}  (str (if report-name "Update" "Save") " Report"))
    (if report-name
      (html "&nbsp;" [:button {:id "new-report" :class "btn btn-warning"} "New"])
      (html "&nbsp;" [:button {:id "new-report" :class "btn btn-warning" :disabled "disabled"} "New"])))
  [:hr]
  )

(defhtml sort-dropdown [data value direction]

    (label  {:class "col-sm-2 control-label" :for data} data value)
     [:div {:class "col-sm-2"}
;;      [:select {:class "form-control"}
;;       [:option ""]
;;       [:option (conj {:value "asc"} (if (= direction "asc" ) {:selected "selected"} {:blank ""})) "Ascending"]
;;       [:option (conj {:value "desc"} (if (= direction "desc" ) {:selected "selected"} {:blank ""})) "Descending"]
;;      ]]
     (drop-down data [["" ""] ["Ascending" "asc"]["Descending" "desc"]] direction)]


)

(defhtml render-sort-dropowns [column-map col-count sort-map]
  (for [m (partition col-count col-count [] column-map)]
           (html [:div {:class "form-group"}
              (for [n m]
                  (sort-dropdown (key n) (val n) (when-not (nil? sort-map) (sort-map (key n)))))])))

(defhtml sort-stream [column-map stream-name sort-map]
  [:h4 "Sort Data"]
  [:p "Select how the data should be sorted"]
  [:hr]
  (form-to {:name "column-sort-data" :class "form-horizontal" :role "form"}
           [:get (str "/streams/data-header/" (ring-codec/url-encode stream-name))]
    (hidden-field "fn" "sort")
    (render-sort-dropowns column-map 3 sort-map)
    (submit-button {:class "btn btn-primary" :id "sort-stream-button"} "Sort Data")
   )
  [:hr]
 )


(defn convert-column-map-to-drop-down-vectors [column-map]
  (map first (map #(assoc {} (name (key %)) (name (val %))) (clj-set/map-invert column-map)))
  )

(defhtml pivot-totals-form [column-map stream-name x-column y-column]

    [:h4 "Pivot Totals"]
    [:p "Total the data by the fields you select and generate a pivot table"]
    [:hr]
    (form-to {:name "column-pivot-data" :class "form-horizontal" :role "form"}
             [:get (str "/streams/data-header/" (ring-codec/url-encode stream-name))]

      (if (and x-column y-column)
        (hidden-field "fn" "unpivot-totals")
        (hidden-field "fn" "pivot-totals"))

      [:div {:class "form-group"}
        (label  {:class "col-sm-2 control-label" :for "x-column"} "x-column" "Column for X values")
        [:div {:class "col-sm-2"}
           (drop-down "x-key" (convert-column-map-to-drop-down-vectors column-map) x-column)]]
       [:div {:class "form-group"}
        (label  {:class "col-sm-2 control-label" :for "x-column"} "y-column" "Column for Y values")
        [:div {:class "col-sm-2"}
           (drop-down "y-key" (convert-column-map-to-drop-down-vectors column-map) y-column)]]
      [:br]

      (if (and x-column y-column)
        (submit-button {:class "btn btn-warning" :id "pivot-totals-button"} "Unpivot Data")
        (submit-button {:class "btn btn-primary" :id "pivot-totals-button"} "Pivot Data"))

     )
    [:hr]
 )

(defhtml stream-manip-nav-tabs [column-map stream-name rel-streams sess-stream-attributes report-data]
  (let [pivotting (not (nil? (sess-stream-attributes :pivot-totals)))
        non-pivot-abiltity-map (if pivotting {:class "disabled"} {})]
    (html [:ul {:class "nav nav-tabs" :id "myTab"}
      ;;[:li [:a {:href "#column-map" :data-toggle "tab"} "Column Names"]]

      [:li (if (not pivotting) {:class "active"} non-pivot-abiltity-map)
           [:a {:href "#totals" :data-toggle "tab"} [:i {:class "icon-th-list"}] "Total"]]
      [:li (if pivotting {:class "active"})
         [:a {:href "#pivot" :data-toggle "tab"}  [:i {:class "icon-repeat"}] "Pivot"]]
      [:li non-pivot-abiltity-map [:a {:href "#filtercols" :data-toggle "tab"} [:i {:class "icon-filter"}] "Filter"]]
      [:li non-pivot-abiltity-map [:a {:href "#related-streams" :data-toggle "tab"}  [:i {:class "icon-group"}] "Related"]]
      [:li non-pivot-abiltity-map [:a {:href "#sorting" :data-toggle "tab"} [:i {:class "icon-sort"}] "Sort"]]
      [:li [:a {:href "#refresh" :data-toggle "tab"}  [:i {:class "icon-refresh"}] "Refresh"]]
      [:li [:a {:href "#save" :data-toggle "tab"}  [:i {:class "icon-save"}] "Save"]]
    ]
    [:div {:class "tab-content"}
        [:div {:class (if pivotting "tab-pane" "tab-pane active") :id "totals"}
         (println "totals wtf " stream-name)
         (totals-form column-map stream-name (sess-stream-attributes :total-cols))
        ]
        [:div {:class (if pivotting "tab-pane active" "tab-pane") :id "pivot"}
         (pivot-totals-form column-map stream-name
                            (when-not (nil? (sess-stream-attributes :pivot-totals)) ((sess-stream-attributes :pivot-totals) :x-key))
                            (when-not (nil? (sess-stream-attributes :pivot-totals)) ((sess-stream-attributes :pivot-totals) :y-key)))
        ]
        [:div {:class "tab-pane" :id "filtercols"}
          (filtercols-form column-map stream-name (sess-stream-attributes :filter-cols))
        ]
        [:div {:class "tab-pane" :id "related-streams"}
          (related-streams column-map stream-name rel-streams (sess-stream-attributes :mapped-streams))
        ]
        [:div {:class "tab-pane" :id "sorting"}
           (sort-stream column-map stream-name (sess-stream-attributes :sort-map))

        ]
        [:div {:class "tab-pane" :id "refresh"}
         (refresh-stream stream-name (if-let [last-refresh (sess-stream-attributes :last-refresh)]
                                       last-refresh
                                       (java.util.Date.)
                                       ))
        ]
        [:div {:class "tab-pane" :id "save"}
            (save-report stream-name (report-data :report-name) (report-data :report_items_per))
        ]

    ]))
    )

















