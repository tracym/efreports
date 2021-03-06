(defproject efreports "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :dependencies [[org.clojure/clojure "1.5.1"]
                 [compojure "1.1.5"]
		 [com.novemberain/monger "1.4.2"]
		 [lib-noir "0.3.3"]
                 [hiccup "1.0.2"]
                 [postgresql/postgresql "8.4-702.jdbc4"]
		 [clojure-csv/clojure-csv "2.0.0-alpha1"]
		
                 [hiccup-bootstrap "0.1.1"]
                 [org.clojure/math.numeric-tower "0.0.2"]
                 [c3p0/c3p0 "0.9.1.2"]
                 [http-kit "2.1.1"]
                 [ring/ring-devel "1.2.1"]
                 [ring/ring-core "1.2.1"]
                 ;;[incanter/incanter-excel "1.5.1"]
                 [clj-excel "0.0.1"]
                 [org.clojure/tools.trace "0.7.5"]
                 [org.clojure/core.memoize "0.5.6"]
                 [org.clojure/data.zip "0.1.1"]
                 [cheshire "5.2.0"]
                 [org.clojure/tools.logging "0.2.6"]
                 [digest "1.4.3"]
                 [clj-http "0.7.8"]
                 [ring-anti-forgery "0.3.0"]
                 ;;[dieter "0.4.1"]
                 [org.clojure/java.jdbc "0.3.5"]
                 [heroku-database-url-to-jdbc "0.2.2"]
                 ]
  :plugins [[lein-ring "0.8.2"]]
  :ring {:handler efreports.handler/app :init efreports.helpers.mongo-init/mongo-connect}
  :min-lein-version "2.0.0"
  :repl-options {:init (do
                        (use 'efreports.helpers.stream-manipulations)
                        (use 'efreports.helpers.data)
                        (use 'efreports.helpers.mongo-init)
                        (require '[efreports.helpers.stream-session :as sess]
                                 '[efreports.models.streams-model :as stream-model]
                                 '[efreports.models.reports-model :as report-model]
                                 '[clojure.string :as clj-string]
                                 '[clojure.set :as clj-set])
                        
                        (use 'clojure.pprint)
                        (mongo-connect)
                         ) :timeout 200000}
  :main ^:skip-aot efreports.handler
  :uberjar-name "efreports-standalone.jar"
  :profiles {:uberjar {:aot :all}})
