var RETRIEVING_DATA_HTML = "<center><div class='spinner'><div class='dot1'></div><div class='dot2'></div></div></center>"

function streamNameFromPath () {
    var qsBegin = location.pathname.indexOf("?");
    var stream;
    if (qsBegin = -1) {
      stream = location.pathname.substring(location.pathname.indexOf("data-header/")).replace("data-header/", "");
    }
    else {
      stream = location.pathname.substring(location.pathname.indexOf("data-header/"), qsBegin).replace("data-header/", "");
    }

  return stream;

}

function currentPageAndPerPage () {
  var currPage = 1;
  var perPage = 10;

  if (location.search) {
    var params = location.search.replace("?", "").split("&");
    var pageParams = params.filter(function(param) {
                        return (param.indexOf("page=") != -1)});

    if (pageParams.length > 0) {currPage = pageParams.shift().replace("page=", "");}

    var perParams = params.filter(function(param) {
                        return (param.indexOf("per=") != -1)});
    if (perParams.length > 0) {perPage = perParams.shift().replace("per=", "");}
  }

  if ($("div[data-report-type]").attr("data-report-type") == "report") {
      perPage = $("table[data-total-results] tr").length - 1;
  }

  return {currentPage: currPage, perPage: perPage};

}

function callPagination () {

  var streamName = $("div[data-stream]").attr("data-stream");
  var totalRes = $("table[data-total-results]").attr("data-total-results");
  var pageInfo = currentPageAndPerPage();
  var bodyUrlPrefix;

  if ($("div[data-report-type]").attr("data-report-type") == "report") {
    bodyUrlPrefix = "/reports/body/";
  }
  else {
    bodyUrlPrefix = "/streams/data-body/"
  }

  var options = {currentPage: pageInfo.currentPage, listContainerClass: 'pagination', numberOfPages: 15,  totalPages: Math.ceil(totalRes / pageInfo.perPage),
                 //pageUrl: function(type, page, current){return "/streams/data-header/"+streamName+"?page="+page+"&per="+pageInfo.perPage}
                 onPageClicked: function(e,originalEvent,type,page){
                   var currentState = history.state;
                   history.pushState(currentState, "", streamName + "?page="+page+"&per="+pageInfo.perPage);
                   //$("div[data-stream]").html("<center><i class=\"icon-spinner icon-spin\"></i> Retrieving Data...</center>");
                   $("table").css("background-color", "#FAFAFA");
                   $.ajax({url: bodyUrlPrefix + streamName+"?page="+page+"&per="+pageInfo.perPage,
                           type: "GET"}).done(function(ret) {
                                                $("div[data-stream]").html(ret);
                                                $("th:visible:not(.add-button)").slice($("table[data-visible-columns]").attr("data-visible-columns"),
                                                                                       $("th:visible:not(.add-button)").length).each(function() {
				                                          $("."+$(this).attr("class")).toggle();});
                                                  $("table").css("background-color", "transparent");
                                                  callPagination();
                                               });
                   }};

  $('.pagination').bootstrapPaginator(options);
}

function loadTable (ajax_return_data) {
  $("div[data-stream]").html(ajax_return_data);
  $("th:visible:not(.add-button)").slice($("table[data-visible-columns]").attr("data-visible-columns"), $("th:visible:not(.add-button)").length).each(function() {
    $("."+$(this).attr("class")).toggle();
	});
  callPagination();
}



$( document ).ready( function() {
  $("abbr.timeago").timeago();
  //$('.selectpicker').selectpicker();
  $("select").selectBoxIt();
  $(".slider").slider();
  $(".slider").css("width", "60%");



  $(document).on('click', "#save-report", function(event) {
    event.preventDefault();
    $.ajax({url:"/reports/create",
            type: "POST",
            data: {stream: $("#stream").val(),
                   report_name: $("#report-name").val(),
                   report_items_per: $("input.slider").val()
                  }}).done(function(ret) {
                    $("div#save").html(ret);
                    $(".slider").slider();
                    $(".slider").css("width", "50%");

                });

  });


  $(document).on('click', "#total-stream-button", function(event) {
    event.preventDefault();
    $("div[data-stream]").html(RETRIEVING_DATA_HTML);


    var streamName = streamNameFromPath();
    var pageInfo = currentPageAndPerPage();
    var currentState = history.state;
    history.pushState(currentState, "", streamName + "?page=1&per="+pageInfo.perPage);


    $.ajax({url: "/streams/data-body/"+ $("div[data-stream]").attr("data-stream") + "?" +
                $("form[name='column-total-data'").serialize(),
            type: "GET"}).done(function(ret) {
                        loadTable(ret);
                     });
   });


  $(document).on('click', "#filter-stream-button", function(event) {
    event.preventDefault();
    $("div[data-stream]").html(RETRIEVING_DATA_HTML);

    $.ajax({url: "/streams/data-body/"+ $("div[data-stream]").attr("data-stream") + "?" +
                $("form[name='column-filter-data'").serialize(),
            type: "GET"}).done(function(ret) {
                        loadTable(ret);
                     });
   });


  $(document).on('click', "#sort-stream-button", function(event) {
    event.preventDefault();
    $("div[data-stream]").html(RETRIEVING_DATA_HTML);

    var streamName = streamNameFromPath();
    var pageInfo = currentPageAndPerPage();
    var currentState = history.state;
    history.pushState(currentState, "", streamName + "?page=1&per="+pageInfo.perPage);

    $.ajax({url: "/streams/data-body/"+ $("div[data-stream]").attr("data-stream") + "?" +
                $("form[name='column-sort-data'").serialize(),
            type: "GET"}).done(function(ret) {
                        loadTable(ret);
                     });
   });

  $(document).on('click', ".filter-op", function(event) {
    event.preventDefault();
    $("div[data-stream]").html(RETRIEVING_DATA_HTML);

    var streamName = streamNameFromPath();
    var pageInfo = currentPageAndPerPage();
    var currentState = history.state;
    history.pushState(currentState, "", streamName + "?page=1&per="+pageInfo.perPage);

    var parentForm = $(this).parent().parent().parent(); //really shitty idea here

    parentForm.children("[name='filter-op-container']").val($(this).attr("data-op"));

    $.ajax({url: "/streams/data-body/"+ $("div[data-stream]").attr("data-stream") + "?" +
                parentForm.serialize(),
            type: "GET"}).done(function(ret) {
                        loadTable(ret);
                     });
   });


  $(document).on('click', "input[id ^= 'map'], input[id ^= 'unmap']", function(event) {
    event.preventDefault();

    $("div[data-stream]").html(RETRIEVING_DATA_HTML);

    $.ajax({url: "/streams/data-body/"+ $("div[data-stream]").attr("data-stream") + "?" +
            $(this).parent().serialize(),
            context: $(this), //we need a reference to the button when the call returns
            type: "GET"}).done(function(ret) {
                        loadTable(ret);

                        //Change the map to an unmap button (and vice versa) after ajax
                        var manipFn = $("table[data-stream]").parent().attr("data-manip-fn");
                        var buttonId = $(this).attr("id");

                        if (manipFn.substring(0, 3) == "map") {
                          $(this).attr("id", buttonId.replace("map", "unmap"));
                          $(this).removeClass("btn-primary");
                          $(this).addClass("btn-warning");
                          $(this).val("Unmap Collection");
                          $(this).parent().children("#fn").val("unmap-stream");


                        } else if (manipFn.substring(0, 5) == "unmap") {
                          $(this).attr("id", buttonId.replace("unmap", "map"));
                          $(this).removeClass("btn-warning");
                          $(this).addClass("btn-primary");
                          $(this).val("Map Collection");
                          $(this).parent().children("#fn").val("map-stream");
                        }

                     $.ajax({url: "/streams/update/column-map-tab/",
                             data: {stream:  $("div[data-stream]").attr("data-stream"),
                                    tab: "total-columns"},
                             type: "POST"}).done(function(ret) {
                                       $("div#totals").html(ret);
                                       $("div#totals .make-switch").bootstrapSwitch();
                                       });
                     $.ajax({url: "/streams/update/column-map-tab/",
                             data: {stream:  $("div[data-stream]").attr("data-stream"),
                                    tab: "filter-columns"},
                             type: "POST"}).done(function(ret) {
                                       $("div#filtercols").html(ret);
                                       $("div#filtercols .make-switch").bootstrapSwitch();
                                       });
                     });
  });

  $(document).on('click', "#report-refresh-button", function(event) {
    event.preventDefault();

    var ajaxUrl = $(this).attr("href");
    $("div[data-stream]").html(RETRIEVING_DATA_HTML);



    $.ajax({url: ajaxUrl,
            type: "POST"}).done(function(ret) {
              loadTable(ret);
            });

    //Manually update the refresh time on the header
    $("abbr.timeago").attr("title", new Date().toJSON().toString());
    $("abbr.timeago").html(jQuery.timeago(new Date()));

  });

  $(document).on('click', 'li.disabled a', function(event) {
    event.preventDefault();
    return false;
  });



   $(document).on('click', "#new-report", function(event) {
     event.preventDefault();
     $("#report-name").val("Enter a name");
     $("#report-name").removeAttr("disabled");
     $("input.slider").slider('setValue', 10);
     $("#save-report").val("Save Report");
   });


  callPagination();
});











