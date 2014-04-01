(ns efreports.controllers.streams-controller
  (:use [compojure.core :only (defroutes GET POST)])
  (:require [clojure.string :as clj-str]
            [ring.util.response :as ring]
            [ring.middleware.params :as params]
            [ring.middleware.keyword-params :as kp]
            [efreports.views.streams.streams-view :as view]
            [efreports.views.streams.streams-markup :as sm]
            [efreports.models.streams-model :as stream-model]
            [efreports.models.reports-model :as report-model]
            [efreports.helpers.data :as data]
            [efreports.helpers.stream-session :as stream-sess]
            [efreports.helpers.stream-manipulations :as stream-manip]
            [efreports.helpers.stream-manip-session-storage :as stream-manip-store]
            [efreports.helpers.export.excel :as xl]
            [efreports.helpers.ring-plumbing :as plumbing]
            [clojure.set :as clj-set]
            [clj-time.core :as clj-time]
            [clj-time.coerce :as clj-time-coerce])
  (:import [java.lang String]
           [java.io ByteArrayOutputStream]

           ))


(def ^:const dflt-first-page 1)
(def ^:const dflt-per-page 10)

(defn get-stream-meta [stream]
  (let [found-stream (stream-model/find-stream-map (stream :stream))]
    (when-not (empty? found-stream) found-stream)))

(defn index [session]
  (println "wtf " session)
  (view/index (stream-model/all-sorted-by-name) (report-model/all-sorted-by-name) (session :username)))

(defn new-stream [stream]
  (view/new-stream stream))

(defn edit-stream [stream-map]
  (let [found-stream (stream-model/find-stream-map (stream-map :stream))]
    (when-not (empty? found-stream)
      (view/edit-stream (assoc found-stream :flash (stream-map :flash)) (stream-map :username)))))

(defn create-stream [stream]
  (prn stream)
  (if-not (empty? stream)
    (let [create-return (stream-model/create stream)
          remove-return (stream-sess/remove-stream (stream :username) (stream :stream-name))]
      (if-not create-return
        (assoc (ring/redirect (str "/streams/new")) :flash "SQL contains illegal keywords")
        (ring/redirect (str "/streams/edit/" (stream :stream-name)))))
    (ring/redirect "/streams")))

(defn delete-stream [stream]
  (stream-model/delete-stream (stream :stream))
  (ring/redirect "/streams")
  )



(defn data-stream-body [stream-params]

  (let [stream (clj-str/replace (stream-params :stream) #"%20" " ")]

    (stream-manip-store/store-request-manip stream stream-params)

    (let [found-stream (stream-model/find-stream-map stream)
          stream-name (found-stream :stream-name)
          colmap (found-stream :column-map)
          user (stream-params :username)

          rs (data/cached-query-memo user (stream-sess/stream-last-refresh user stream-name)
                                             (found-stream :sql))

          stream-attr (stream-sess/stream-attributes user stream-name)

          manip-rs     (stream-manip/apply-stored-manip stream-attr rs)


          ;;In the case of pivot tables we can end up with nil in the list of rs-keys
          ;;for now we are going to filter nil out. Administrators should shy away
          ;;from creating streams that return ni values if they want users to be able to pivot them
          supplanted-manip-data (merge {:name stream-name :user user
                                        :rs-keys (keys (first manip-rs))} stream-attr)


          manip-cmo  (if-not (stream-sess/stream-empty? user stream-name)
                                             (stream-manip/manip-column-map-ordering supplanted-manip-data colmap)
                                             (stream-sess/column-map-ordering user stream-name))


          write-session-result (stream-sess/write-stream-keys user stream
                                                              :column-map-ordering manip-cmo)
          related-streams (stream-model/related-streams (found-stream :stream-name) (found-stream :key-columns))


          manip-column-map (stream-manip/manip-column-map
                            supplanted-manip-data manip-cmo)




          per (if (contains? stream-params :per)
                (data/parse-int (stream-params :per))
                dflt-per-page)

          page (if (contains? stream-params :page)
                (data/parse-int (stream-params :page))
                 dflt-first-page)
          paginated-rs (stream-manip/paginate-stream-rs manip-rs per page)]

            (if (contains? stream-params :format)
              (when (= (stream-params :format) "xls")
                (let [wb (xl/seq-map->workbook stream-name manip-rs)
                      wb-bytes (xl/workbook->ByteArryInputStream wb)]
                  (ring/content-type (ring/response wb-bytes) "application/vnd.ms-excel")))

              (view/stream-data-body stream-params paginated-rs per page (count manip-rs)
                                     related-streams
                                     manip-column-map
                                     (if (contains? stream-params :fn)
                                               (stream-params :fn) ""))))))

;Start by rendering the blank data stream view template. From here create more
;methods to update the progress of data retrieval and then render the data
;part of the async stream retrieval functionality
(defn data-stream-header [stream-params]

    (stream-manip-store/store-request-manip (stream-params :stream) stream-params)
    (let [found-stream (get-stream-meta stream-params)]
       (stream-sess/init-stream-session (stream-params :username) (found-stream :stream-name))
       (let [mod-stream-params (if-let [ms ((stream-sess/stream-attributes
                                             (stream-params :username) (found-stream :stream-name)) :mapped-streams)]
                                (if-not (empty? ms)
                                  (merge (dissoc found-stream :column-map)
                                           {:column-map
                                            (merge (found-stream :column-map)
                                            (reduce merge (map (fn [x] ((stream-model/find-stream-map x) :column-map)) ms)))})
                                  found-stream
                                  )
                                  found-stream)]
        (println (str "modified stream param " mod-stream-params))

        (if (contains? stream-params :format)
              (when (= (stream-params :format) "xls")
                 (data-stream-body stream-params))

              (view/data-stream-header (merge mod-stream-params (select-keys stream-params [:username]))
                              (stream-model/related-streams (found-stream :stream-name) (found-stream :key-columns))
                              "/javascript/stream-data.js" (if (contains? stream-params :fn)(stream-params :fn) "render"))))))




(defn update-stream [stream-params]
  (prn stream-params)
  (let [found-stream (stream-model/find-stream-map (stream-params :stream-name))]
      (let [update-result (stream-model/update found-stream stream-params)]
        (if update-result
          (ring/redirect (str "/streams/data-header/" (found-stream :stream-name)))
          (assoc (ring/redirect (str "/streams/edit/" (found-stream :stream-name))) :flash "SQL contains illegal keywords")))))

(defn update-column-map [colmap-data]
  (println "when is this happening?")
  (let [stream-map (select-keys colmap-data [:stream])]
      (let [found-stream (stream-model/find-stream-map (stream-map :stream))]
        (when-not (empty?  found-stream)
          (let [keycols (stream-manip-store/extract-keycol-params colmap-data)
                colmaps (into {} (filter
                                   #(when-not
                                      (. (str (key %)) startsWith ":key_col_ind_") %) colmap-data))]
          (stream-model/update-keycols found-stream keycols)
          (stream-model/update-column-map found-stream colmaps)
          ;;overwrite the session column map ordering with new column
          ;;map orderings
          (stream-sess/init-stream-session (colmap-data :username) (found-stream :stream-name))
          (stream-sess/write-stream-keys (colmap-data :username) (found-stream :stream-name) :column-map-ordering
                                         (stream-manip/init-column-map-ordering ((stream-model/find-stream-map (found-stream :stream-name)) :column-map) 0))
          (ring/redirect "/streams"))))))


;;jq: $.ajax({url:"/streams/update-visibility/", type: "POST", data:
;; {stream: "test-rms-id", column_name: "last_modified", visibility: false, column_count: "6" }}).done(function( msg ) {alert( "Data Saved: " + msg );});

(defn update-column-visibility [update-params]
  (let [stream (clj-str/replace (update-params :stream) #"%20" " ")
        visible (Boolean/valueOf (update-params :visibility))
        col-count (data/parse-int (update-params :column_count))]

    (if visible
      (stream-sess/move-column-to-position (update-params :username) stream
                                    ((stream-sess/stream-attributes (update-params :username) stream) :column-map-ordering)
                                    (update-params :column_name) col-count)
      (stream-sess/move-column-to-back (update-params :username) stream
                                    ((stream-sess/stream-attributes (update-params :username) stream) :column-map-ordering)
                                    (update-params :column_name)))
    (stream-sess/write-stream-keys (update-params :username) stream :column-display-count col-count)
    (data-stream-body (merge {:stream stream} (select-keys update-params [:per :page :username]))) ;;preserve pagination
  ))

(defn update-column-map-tab [update-params]
  (let [stream (clj-str/replace (update-params :stream) #"%20" " ")
        tab (update-params :tab)
        stream-attrs (stream-sess/stream-attributes (update-params :username) stream)
        total-cols (stream-attrs :total-cols)
        filter-cols (stream-attrs :filter-cols)
        mapped-streams (stream-attrs :mapped-streams)
        found-stream  (stream-model/find-stream-map stream)
        base-column-map (found-stream :column-map)
        effective-colmap   (into (sorted-map) (if-not (empty? mapped-streams)
                                                  (merge base-column-map (reduce merge (map (fn [x] ((stream-model/find-stream-map x) :column-map)) mapped-streams)))
                                                         base-column-map))]

    (if (= tab "total-columns")
      (sm/totals-form effective-colmap stream total-cols)

      (if (= tab "filter-columns")
        (sm/filtercols-form effective-colmap stream filter-cols)))

  )
)

(defroutes routes
  (GET  "/streams" {session :session} (plumbing/wrap-session-and-route session session index))

  (GET "/streams/new" {stream-params :params session :session flash :flash}
        (plumbing/wrap-session-and-route session (assoc stream-params :flash flash) new-stream))

  (GET "/streams/edit/:stream" {stream-params :params session :session flash :flash}
       (plumbing/wrap-session-and-route session (assoc stream-params :flash flash) edit-stream))

  (GET "/streams/data-header/:stream*" {stream-params :params session :session}
       (plumbing/wrap-session-and-route session stream-params data-stream-header))

  (GET "/streams/data-body/:stream*" {stream-params :params session :session}
       (plumbing/wrap-session-and-route session stream-params data-stream-body))

  (POST "/streams/create" {stream-params :params session :session}
        (plumbing/wrap-session-and-route session stream-params create-stream))

  (POST "/streams/update" {stream-params :params session :session}
        (plumbing/wrap-session-and-route session stream-params update-stream))

  (POST "/streams/update-visibility/" {update-params :params session :session}
        (plumbing/wrap-session-and-route session update-params update-column-visibility))

  (GET "/streams/delete/:stream" {update-params :params session :session}
         (plumbing/wrap-session-and-route session update-params delete-stream))

  (kp/wrap-keyword-params
   (POST "/streams/update/column-map-tab/"  {stream-params :params session :session}
                                  (plumbing/wrap-session-and-route session stream-params update-column-map-tab)))

  (kp/wrap-keyword-params
  (POST "/streams/update/column-map" {stream-params :params session :session}
                                  (plumbing/wrap-session-and-route session stream-params update-column-map))))
