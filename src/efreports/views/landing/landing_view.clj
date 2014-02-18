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
     [:p "Ef Reports let's you sort, filter, group, and join data in the browser. Sign in to give it a try."]
     [:p [:a {:class "btn btn-primary btn-lg" :role "button" :href "/login"} "Sign In &raquo;"]]
     ]]
   [:div {:class "container"}
    [:div {:class "row"}
     [:div {:class "col-md-4"}
      [:h3 "A Thought Experiment"]
      [:p "If anyone could manipulate data like report developers, would enterprise reports be necessary? Ef Reports allows users to slice up their data any way they want and see the results in seconds.
       "]
      ]

     [:div {:class "col-md-4"}
      [:h3 "Features"]
         [:ul
          [:li "Sort, filter, group, and join data in the browser"]
          [:li "Written in Clojure for performance and scalability"]
          [:li "Users determine when to pull data from the database"]
          [:li "All data is manipulated in memory"]
          ]

     ]
     [:div {:class "col-md-4"}
      [:h3 "Roadmap"]
      [:p "Ef Reports is very much a side project and the purpose of this demo is to guage what kind of interest there is for a tool like this. The amount of interest (if any) and
         and the feedback I receive will determine how I move forward with this. Should this be free software or should I trash it?" [:a {:href "mailto:efreportsdemo@gmail.com"} "Let me know!"]]
     ]
     [:div {:class "col-md-4"}
      [:h3 "Known Issues"]
      [:ul
       [:li "Forms that accept direct SQL input should be moved or removed because security"]
       [:li "The name of this project probably should be changed"]
       [:li "Joining or \"mapping\" data behaves more like a left join than an inner join"]
       [:li "Pivot tables can only be generated from Collections that have not been manipulated"]
       [:li "Nulls and data types other than numbers and strings are not handled reliably. When creating Collections, liberal use of CAST and COAELESCE are your best bet"]
       [:li "Mapping result sets causes noticeable performance degredation"]
       ]
      ]
     [:div {:class "col-md-4"}
      [:h3 "Feedback"]
      [:p "Email " [:a {:href "mailto:efreportsdemo@gmail.com"} "efreportsdemo@gmail.com."] " Your feedback is important."]
      ]
    ]]

)

(defn landing-page []
  (layout/common "Welcome to Ef Reports" nil  (landing-html)))
