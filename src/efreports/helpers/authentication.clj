(ns efreports.helpers.authentication
  (:require [clojure.string :as clj-string]
            [org.httpkit.client :as http]
            [digest :as digest])
  (:use [clj-xpath.core]))
