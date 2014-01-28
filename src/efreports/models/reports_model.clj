(ns efreports.models.reports-model
  (:require [monger.core :as m]
  					[monger.collection :as mc]
            [monger.query :as mq]
            [clj-time.core :as clj-time]
            [clj-time.coerce :as clj-time-coerce]
            )
  (:use monger.operators)
  (:import [org.bson.types ObjectId]
           [com.mongodb DB WriteConcern]))

(defn reports-connect [dbname]
  (let [uri (get (System/getenv) "MONGODB_URI" "mongodb://127.0.0.1/")]
    (m/connect-via-uri! uri))
  (m/set-db! (m/get-db dbname)))


(defn now [] (java.util.Date.))

(defn create-report [report-name username manip-map items-per]
  (reports-connect "efreports")
  (mc/insert "reports" { :_id (ObjectId.)
                           :report-name report-name
                           :author username
                           :stream-manipulations manip-map
                           :items-per-page items-per
                           :base-stream (manip-map :name)
                           :date-created (now)
                           :last-refresh (now)
                           }))


(defn find-latest-report-map-by-user-and-stream
  [user stream]
  (reports-connect "efreports")
  (first
    (mq/with-collection "reports"
      (mq/find {:base-stream stream :author user})
      (mq/fields [:date-created :report-name :items-per-page])
      (mq/sort (array-map :date-created -1))
      (mq/limit 1))))


(defn find-latest-report-map-by-user-and-report
  [user report-name]
  (reports-connect "efreports")
  (first
    (mq/with-collection "reports"
      (mq/find {:report-name report-name :author user})
      (mq/fields [:date-created :report-name :items-per-page :stream-manipulations :last-refresh :base-stream])
      (mq/sort (array-map :date-created -1))
      (mq/limit 1))))

(defn all []
  (reports-connect "efreports")
  (mc/find-maps "reports"))

(defn all-sorted-by-name []
  (reports-connect "efreports")
  (mq/with-collection "reports"
      (mq/fields [:date-created :report-name :base-stream])
      (mq/sort (array-map :report-name 1))))


(defn find-report-map [report]
  (mc/find-one-as-map "reports" {:report-name report}))

(defn update-report [report stream-manipulations items-per-page]
  (reports-connect "efreports")
  (mc/update "reports" {:_id (report :_id)}
                           {$set {:stream-manipulations stream-manipulations
                                  :items-per-page items-per-page
                                  }}))

(defn refresh-report [report]
  (reports-connect "efreports")
  (mc/update "reports" {:_id (report :_id)}
                           {$set {:last-refresh (now)}}))








