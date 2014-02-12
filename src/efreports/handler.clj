(ns efreports.handler
  (:use [compojure.core]
  	    [ring.middleware.params :only [wrap-params]]
        [hiccup.bootstrap.middleware]
        [ring.middleware.file]
        [ring.middleware.resource]
        [ring.middleware.flash]
        [ring.middleware.anti-forgery]
        [hiccup.middleware :only (wrap-base-url)]
        [monger.ring.session-store :only [session-store]]
        [ring.middleware.session]
        [org.httpkit.server :only [run-server]]
        [efreports.views.layout :only (common)]
        [clojure.tools.logging :only (info error)]
        [clojure.pprint]
        [hiccup.core :only (html)]
        [ring.util.response])
  (:require [compojure.handler :as handler]
            [compojure.route :as route]
            [ring.middleware.reload :as reload]
            [ring.middleware.params :only [wrap-params]]
            [efreports.controllers.streams-controller :as streams-controller]
            [efreports.models.streams-model :as streams-model]
            [efreports.helpers.html-components :as hc]
            [efreports.helpers.authentication :as auth]
            [efreports.controllers.reports-controller :as reports-controller]
            [efreports.controllers.dashboard-controller :as dashboard-controller]
            [efreports.views.landing.landing-view :as landing]
           ;; [dieter.core :as dieter]

            )
  (:gen-class))


(defn authenticate
  [session login-params]

    (assoc (redirect "/streams") :session (assoc session :username (login-params :username)))
    ;;(assoc (redirect "/login") :flash "Login Failed!")
)

(defn logout []
  (assoc (redirect "/login") :session nil))

(defn wrap-failsafe [handler]
  (fn [req]
    (try
      (handler req)
      (catch Exception e
        {:status 500
         :headers {"Content-Type" "text/html"}
         :body (html (common "Error" nil "We're sorry, something went wrong."))}))))




(defroutes app-routes
  (route/resources "/")
  (GET "/" {} (landing/landing-page))
  (GET "/login" {msg :flash} (common "Login" nil (html (hc/login-form msg))))
  (GET "/logout" {session :session} (logout))
  (POST "/auth" {session :session login-params :params} (authenticate session login-params))
  ;;(GET "/session" {session :session} (view-session session))
  streams-controller/routes
  reports-controller/routes
  dashboard-controller/routes
  (route/not-found (common "Not Found" nil (html [:h1 "Not Found"]))))

;; (def app
;;    (wrap-params (reload/wrap-reload (wrap-bootstrap-resources
;;                                     (with-security authorize
;;     (handler/site app-routes {:session-store (session-store "sessions")}))))))


(def app

   ;;(wrap-bootstrap-resources)
   (wrap-resource

    (handler/site app-routes {:session-store (session-store "sessions")})  "resources/public"))


(defn -main [& args]
   (let [site app]
    ;;(dieter/init {:cache-mode :production})
    (run-server site {:port (Integer. (or (System/getenv "PORT") "8080"))})))
