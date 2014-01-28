(ns efreports.helpers.tag
	(:use [hiccup.core]))

;; tag and then data
(defn map-tag [tag xs]
	(map (fn [x] [tag x]) xs))

(defn map-tag-class [tag xs cs]
  (map (fn [x,c] [tag {:class c} x]) xs cs))
