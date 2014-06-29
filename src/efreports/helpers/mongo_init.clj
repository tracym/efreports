(ns efreports.helpers.mongo-init
  (:require [monger.core :as m]))

(defn parse-mongo-uri [mongo-uri]
    (let [stripped (.substring mongo-uri 10)
          user (.substring stripped 0 (.indexOf stripped  ":"))
          password (.substring mongo-uri (+ (.indexOf mongo-uri user) (+ 1 (count user))) (.indexOf mongo-uri "@"))
          db (.substring mongo-uri (+ 1  (.lastIndexOf mongo-uri "/")))
          ]
      (assoc {} :user user :password password :db db)))

(defn local-mongo []
  (m/connect!)
  (m/set-db! (m/get-db "efreports")))

(defn heroku-mongo [mongo-uri]
  (let [auth-map (parse-mongo-uri mongo-uri)]
    (m/connect-via-uri! mongo-uri)
    (m/use-db! (auth-map :db))
    (m/authenticate (m/get-db (auth-map :db)) (auth-map :user) (.toCharArray (auth-map :password)))))

(defn mongo-connect []
  (if-let [mongo-url (System/getenv "MONGOHQ_URL")]
    (heroku-mongo mongo-url)
    (local-mongo)))
