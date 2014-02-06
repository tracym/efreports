(ns efreports.views.landing.landing-view
  (:use [hiccup.def]
       [hiccup.core]
         [hiccup.form :only [form-to check-box label text-area
                                    text-field hidden-field submit-button]]
         [hiccup.page :only [include-js]]
       )
  (:require [efreports.views.layout :as layout]
            ;;[efreports.views.streams.streams-markup :as sm]
            ;;[efreports.helpers.html-components :as hc]
            [clojure.string :as clj-string]
           )
  )

(defhtml landing-html []

   [:div {:class "jumbotron"}
    [:div {:class"container"}
     [:h2 "Fast, Dynamic Data Manipulation in the Browser"]
     [:p "Ef Reports is an application that allows users without knowledge of SQL to sort, filter, group, and join data in the browser using a clean, modern
      interface built with Bootstrap. Sign in to demo the software or watch the screencast to see if Ef Reports would be more convenient for you."]
     [:p [:a {:class "btn btn-primary btn-lg" :role "button" } "Sign In &raquo;"]]
     ]]
   [:div {:class "container"}
    [:div {:class "row"}
     [:div {:class "col-md-4"}
      [:h3 "A Thought Experiment"]
      [:p "If the end users of enterprise reporting software had the ability to quickly manipulate data in a way similar to report developers, would enterprise reports be necessary?
           After all, if the goal of a report is to present information from a data source to people who want to know, why must this be accomplished
           with endless, mind-numbing \"Report Design\" widgets and all the other trappings of enterprise software? Are end users really satisfied as passive consumers of information
           when they can be empowered to manipulate pre-defined collections of data to find the information they want when they want it?
       "]
      ]
     [:div {:class "col-md-4"}
      [:h3 "No Hassles - For Anybody"]
      [:p "Let administrators create collections using only SQL. Let end users manipulate them and save the results. The results of any series of manipulations can be downloaded into an Excel file at any time."]
      ]
     [:div {:class "col-md-4"}
      [:h3 "Roadmap"]
      "This is still very much a side project and the purpose of this demo app is to guage what kind of interest is out there for a tool of this sort. The amount of interest (if any) and
      and the feedback I receive will determine how I move forward with this. Should this be free software or should I use this as an opportunity leave my current job? Let me know!"
     ]
     [:div {:class "col-md-4"}
      [:h3 "Known Issues"]

      ]

    ]

)

(defn landing-page []
  (layout/common "Welcome" nil  (landing-html)))
