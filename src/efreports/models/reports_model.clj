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



(defn parse-mongo-url [mongo-url]
  (let [stripped (.substring mongo-url 10)
        user (.substring stripped 0 (.indexOf stripped  ":"))
        password (.substring mongo-url (+ (.indexOf mongo-url user) (+ 1 (count user))) (.indexOf mongo-url "@"))
        db (.substring mongo-url (+ 1  (.lastIndexOf mongo-url "/")))
        ]
    (assoc {} :user user :password password :db db)))


(defn reports-connect [dbname]
  (let [uri (get (System/getenv) "MONGOHQ_URL" (str  "mongodb://foo:bar@127.0.0.1/" dbname))
        auth-map (parse-mongo-url uri)
        ]

    (m/connect-via-uri! uri)
    (m/use-db! (auth-map :db))
    (m/authenticate (m/get-db (auth-map :db)) (auth-map :user) (.toCharArray (auth-map :password)))
    )
  )



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


(defn find-latest-report-map-by-report
  [user report-name]
  (reports-connect "efreports")
  (first
    (mq/with-collection "reports"
      (mq/find {:report-name report-name})
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








