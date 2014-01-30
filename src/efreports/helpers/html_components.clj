(ns efreports.helpers.html-components
	(:use [hiccup.def :only (defhtml)]
        [hiccup.page :only (html5 include-css include-js)]
        [hiccup.form :only (form-to check-box label text-area text-field password-field hidden-field submit-button)]
        [hiccup.core]
        [ring.util.anti-forgery]
        [hiccup.bootstrap.page]
        [efreports.helpers.data]
        [efreports.helpers.tag])
  (:require [clojure.math.numeric-tower :as math])
  (:import [java.lang String]))


(defhtml bootstrap-dd-button [action]
    [:div {:class"btn-group" :id "Add"}
      [:a {:class "btn btn-default dropdown-toggle" :data-toggle "dropdown" :href "#"} (str action " ")
        [:span {:class "caret"}]]

      [:ul {:class "dropdown-menu"}]
    ])

(defhtml boostrap-form-control [label-value control-name control-value control-type]
  ;;horizontal twitter bootstrap form element
  [:div {:class "form-group"}
    (label {:class "control-label col-lg-2"} control-name label-value)
    [:div {:class "col-lg-10"}
      (control-type {:class "form-control"} control-name control-value)]
  ])

(defhtml bootstrap-progress-bar [bar-id]
  [:div {:class "progress progress-striped active"}
    [:div {:class "bar" :id bar-id :style "width:0%;"}]
  ]
)

(defhtml pagination-bar [stream-name per-page page-num rs-count]
      (html ;;[:div
       [:ul {:class "pagination"}
       ; [:li [:a {:href "#"} "&laquo;"]]
;;           (for [m (page-label-map rs-count per-page page-num)]
;;                 (html [:li {:class (str "paginator" (if (= (key m) page-num) (str " active")(str "")))}
;;                   [:a {:href (str "/streams/data-header/" stream-name "?page=" (val m) "&per=" per-page)} (val m)]]))
             (for [n (range 1 (int (math/ceil (/ rs-count per-page))))]
               [:li {:class (str "" (if (= n page-num) (str "active")(str "")))}
                 [:a {:href "#"
                      ;;(str "/streams/data-header/" stream-name "?page=" n "&per=" per-page)

                      } (str n)]
                ])
        ;[:li [:a {:href "#"} "&raquo;"]]
    ]))

;; (defhtml pagination-js [stream-name per page total]
;;   [:script {:type "text/javascript"}
;;     ;; needed to add listContainerClass to opts for bootstrap 3. This does not appear to be supported, but it works
;;     (str (fmt "var options = {currentPage: #{page}, listContainerClass: 'pagination', numberOfPages: 15,  totalPages:#{(int (math/ceil (/ total per)))},")
;;          (fmt "pageUrl: function(type, page, current){return \"/streams/data-header/#{stream-name}?page=\"+page+\"&per=#{per}\"}};")
;;          "$('.pagination').bootstrapPaginator(options);")])

(defhtml accordion-group [title content]
  [:div {:class "accordion-group"}
    [:div {:class "accordion-heading"}
      [:a {:class "accordion-toggle" :data-toggle "collapse"
           :data-parent "#accordion-main" :href "#collapseOne"} title]
    ]
    [:div {:id "collapseOne" :class "accordion-body collapse in"}
      [:div {:class "accordion-inner"}
        content
      ]
    ]
  ])

(defhtml panel-container
  "bootstrap 3 panel"
  [title content]
  [:div {:class "panel panel-default"}
    [:div {:class "panel-heading"}
     [:h1 {:class "panel-title"} title]]
    [:div {:class "panel-body"} content]
  ]
)

(defhtml navbar [username]
   [:div {:class "navbar navbar-inverse navbar-static-top" :role "navigation"}
      ;;[:div {:class"navbar-inner"}
        [:div {:class "container"}
         [:div {:class "navbar-header"}
          [:button {:type "button" :class "navbar-toggle"
                   :data-toggle "collapse" :data-target ".navbar-collapse"}
            [:span {:class "icon-bar"}]
            [:span {:class "icon-bar"}]
            [:span {:class "icon-bar"}]
          ]
          [:a {:class "navbar-brand" :href "#"}
           "Eff Reports"

           ]]

          [:div {:class "collapse navbar-collapse"}
            [:p {:class "navbar-text navbar-right"}
             (if username
               (html "Logged in as " [:a {:href "#" :class "navbar-link"} username]
                " | "[:a {:href "/logout" :class "navbar-link"} "Log out "  [:i {:class "icon-signout"}]])
               (html  [:a {:href "/login" :class "navbar-link"} "Log in "  [:i {:class "icon-signin"}]]))
             ]
            [:ul {:class "nav navbar-nav"}
              [:li {:class "active"} [:a {:href "/"} "Home"]]
              [:li [:a {:href "#"} "About"]]
            ]
          ];;.nav-collapse -->
        ]
      ]
    )


(defhtml login-form [msg]
  [:div {:id "login-form" :class "row"}
   [:div {:class "col-md-6 col-md-offset-4"}
    (if msg
      (html [:div {:class "alert alert-danger alert-dismissable"}
                [:button {:type "button" :class "close" :data-dismiss "alert" :aria-hidden "true"} "&times;"]
                  [:strong msg]
                  " You could not be logged in. Ensure that your AccessNet username and password
                   are correct and then try again."]))
    [:form {:name "stream-data" :role "form" :class "form-horizontal" :method "POST" :action "auth"}
    
     [:legend "Login Information"]
            [:div {:class "form-group"}
              (label {:class "control-label col-lg-2"} "username" "Username")
              [:div {:class "col-lg-4"}
                  (text-field {:class "form-control"} "username" "")]]
             [:div {:class "form-group"}
              (label {:class "control-label col-lg-2"} "password" "Password")
              [:div {:class "col-lg-4"}
                  (password-field {:class "form-control"} "password")]]

             [:div {:class "form-group"}
              (submit-button {:class "btn btn-primary"} "Login " )]]]])



