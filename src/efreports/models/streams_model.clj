(ns efreports.models.streams-model
  (:require [monger.core :as m]
  					[monger.collection :as mc]
            [monger.query :as mq]
            [efreports.helpers.data :as h])
  (:use monger.operators)
  (:import [org.bson.types ObjectId]
           [com.mongodb DB WriteConcern]))


(defn now [] (java.util.Date.))

(defn sql-contains-illegal-keywords? [sql]
  (re-find #"(?i)\binsert\b|\bdelete\b|\bupdate\b" sql))

(defn parse-mongo-url [mongo-url]
  (let [stripped (.substring mongo-url 10)
        user (.substring stripped 0 (.indexOf stripped  ":"))
        password (.substring mongo-url (+ (.indexOf mongo-url user) (+ 1 (count user))) (.indexOf mongo-url "@"))
        db (.substring mongo-url (+ 1  (.lastIndexOf mongo-url "/")))
        ]
    (assoc {} :user user :password password :db db)))



(defn streams-connect [dbname]
  (let [uri (get (System/getenv) "MONGOHQ_URL" (str  "mongodb://foo:bar@127.0.0.1/" dbname))
        auth-map (parse-mongo-url uri)
        ]
    
    (m/connect-via-uri! uri)
    (m/use-db! (auth-map :db))
    (m/authenticate (m/get-db (auth-map :db)) (auth-map :user) (.toCharArray (auth-map :password)))
    )
  )

(defn all []
  (streams-connect "efreports")
  (mc/find-maps "streams"))

(defn all-sorted-by-name []
  (streams-connect "efreports")
  (mq/with-collection "streams"
      (mq/fields [:date-created :stream-name :description])
      (mq/sort (array-map :stream-name 1))))


(defn find-stream-map [stream]
  (mc/find-one-as-map "streams" {:stream-name stream}))

(defn find-stream-id [stream]
  (streams-connect "streams")
)

(defn delete-stream [stream]
  (mc/remove "streams" {:stream-name stream}))

(defn find-map-exec-sql [user last-refresh stream-name]
   (streams-connect "efreports")
   (when (not (empty? stream-name))
     (if-let [stream (find-stream-map stream-name)]
       (h/cached-query-memo user last-refresh (stream :sql))
       nil)))

(defn find-stream-key-cols [stream-name]
  (streams-connect "efreports")
  (when (not (empty? stream-name))
    ((find-stream-map stream-name) :key-columns)))

(defn related-streams [stream-name keycols]
  (streams-connect "efreports")
  (when (not (empty? keycols))
    (mc/find-maps "streams" {$and [{:key-columns {$in keycols}} {:stream-name {$ne stream-name}}]})))

(defn stream-column-map [sql]
  ;;(h/sort-column-map
   (let [rs-keys (keys (first (h/exec-sql sql)))]
    (zipmap rs-keys (map name rs-keys))))

(defn update [old-stream new-stream]
  (streams-connect "efreports")
  (if (sql-contains-illegal-keywords? (new-stream :sql))
    nil
    (mc/update "streams" {:_id (old-stream :_id)} (merge (dissoc new-stream :flash) {:column-map (stream-column-map (new-stream :sql))}))))

(defn update-column-map [stream colmap-post]
    (streams-connect "efreports")
    ;;(prn (select-keys colmap-post (for [[k,v] colmap-post :when (not= k :stream-name)] k)))
    (mc/update "streams" {:_id (stream :_id)}
                                  {$set {:column-map ;;(h/sort-column-map
                                                       (select-keys colmap-post
                                                                   (for [[k,v] colmap-post
                                                                    :when (and (not= k :stream-name)
                                                                                (not= k :stream)
                                                                                (not= k :username)
                                                                                (not= k :flash))

                                                                        ] k))}}))

(defn update-keycols [stream keycols]
    (streams-connect "efreports")
    (mc/update "streams" {:_id (stream :_id)}
                           {$set {:key-columns keycols}})
)

(defn create [stream]
  (streams-connect "efreports")
  (if (sql-contains-illegal-keywords? (stream :sql))
    nil
    (mc/insert "streams" { :_id (ObjectId.)
                             :stream-name (stream :stream-name)
                             :description (stream :description)
                             :sql (stream :sql)
                             :column-map ;;(h/sort-column-map
                                          (stream-column-map (stream :sql))
                             :date-created (now)
                             })))


