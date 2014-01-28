(ns efreports.helpers.ring-plumbing
  (:require [clojure.string :as clj-str]
            [ring.util.response :as ring])
  )


(defn wrap-session-and-route [session params route-fn]
  (let [username (select-keys session [:username])]
    (if (empty? username)
      (ring/redirect "/login")

      (let [wrapped-params (merge params username)]
        (route-fn wrapped-params)))))

