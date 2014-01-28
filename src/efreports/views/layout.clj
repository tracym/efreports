(ns efreports.views.layout
  (:use [hiccup.core :only (html)]
        [hiccup.def :only (defhtml)]
        [hiccup.page :only (html5 include-css include-js)]
        [hiccup.bootstrap.page]
        [efreports.helpers.html-components]))

(defn common [title user & body]
  (html5
    [:head
      [:meta {:charset "utf-8"}]
      [:meta {:http-equiv "X-UA-Compatible" :content "IE=edge,chrome=1"}]
      [:meta {:name "viewport" :content "width=device-width, initial-scale=1, maximum-scale=1"}]
      [:title title]
      ;;(include-bootstrap)
      (include-js "//ajax.googleapis.com/ajax/libs/jquery/1.10.2/jquery.min.js")
      (include-js "//ajax.googleapis.com/ajax/libs/jqueryui/1.10.3/jquery-ui.min.js")
      ;;(include-js "/javascript/bootstrap.min.js")
      (include-js "//netdna.bootstrapcdn.com/bootstrap/3.0.0/js/bootstrap.min.js")
      (include-js "/javascript/bootstrap-paginator.min.js")
      (include-js "/javascript/jquery.timeago.js")
      (include-js "http://cdnjs.cloudflare.com/ajax/libs/jquery.selectboxit/3.7.0/jquery.selectBoxIt.min.js")
      (include-js "/javascript/bootstrap-slider.js")
      ;;below this is where any js written specifically for this app should live
      (include-js "/javascript/base.js")
      (include-js "/javascript/filter-pills.js")
      (include-js "/javascript/bootstrap-switch.js")
      (include-js "/javascript/table.js")


      (include-css "http://fonts.googleapis.com/css?family=Droid+Sans:400,700")
      (include-css "http://fonts.googleapis.com/css?family=Arvo:400,700")
      (include-css "//netdna.bootstrapcdn.com/bootstrap/3.0.0/css/bootstrap.min.css")
      (include-css "//netdna.bootstrapcdn.com/bootstrap/3.0.0/css/bootstrap-theme.min.css")
      ;;(include-css "/stylesheets/magic-bootstrap.css")
      (include-css "/stylesheets/font-awesome.min.css")
      (include-css "/stylesheets/base-layout.css")
      (include-css "/stylesheets/filter-pills.css")
      (include-css "/stylesheets/table.css")
      (include-css "/stylesheets/bootstrap-switch.css")
      ;;(include-css "/stylesheets/bootstrap-select.min.css")
      (include-css "http://cdnjs.cloudflare.com/ajax/libs/jquery.selectboxit/3.7.0/jquery.selectBoxIt.css")
      (include-css "/stylesheets/slider.css")
      (include-css "/stylesheets/chasing-dots.css")
      "<!-- HTML5 Shim and Respond.js IE8 support of HTML5 elements and media queries -->
        <!-- WARNING: Respond.js doesn't work if you view the page via file:// -->"
          (include-js "https://oss.maxcdn.com/libs/html5shiv/3.7.0/html5shiv.js")
          (include-js "https://oss.maxcdn.com/libs/respond.js/1.3.0/respond.min.js")
       "<![endif]-->"
     ]
  [:body
    (navbar user)
    [:div {:class "container"}
      [:div {:class "row"}
        [:div {:class "col-md-12"}
         
         body]]]

;;    (include-js "/javascript/jquery.simplePagination.js")
;;    (include-js "/javascript/pagination.js")
   ]))
