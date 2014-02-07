$( document ).ready( function() {


	$("th:visible").slice(0, $("table[data-visible-columns]").attr("data-visible-columns")).each(function() {
		$("."+$(this).attr("class")).toggle();
	});

	$("li.paginator:gt(10):not(:last())").toggle();

	$("li.paginator").click(function(){
		$("li.paginator:gt(10):not(:last()):not(:visible)").toggle();
	});

	// $("th").hover(function(){
	// 	$("."+$(this).attr("class")+" > span").toggle();
	// });

	// $("th > a").click(function() {
	// 	$("."+$(this).parent().attr("class").split(" ").join(".")).toggle();
	// });

	$(document).on('click', "th > a", function() {
		$("."+$(this).parent().attr("class").split(" ").join(".")).toggle();
    var bodyUrlPrefix;

    if ($("div[data-report-type]").attr("data-report-type") == "report") {
      bodyUrlPrefix = "/reports/";
    }
    else {
      bodyUrlPrefix = "/streams/"
    }

    $.ajax({url: bodyUrlPrefix + "update-visibility/",
            type: "POST",
            data: {stream: $("div[data-stream]").attr("data-stream"), column_name: $(this).parent().attr("class"),
                   visibility: false, column_count: $("th:visible:not(.add-button)").length}});
	});

	// $("#Add > a.btn").click(function(event) {
	// 	event.preventDefault();
	// 	$("#Add > ul").html("");
	// 	$("th:not(:visible)").each(function () {
	// 		$("#Add > ul").append("<li><a href=\"#\">"+($(this).attr("class"))+"</a></li>");
	// 	});
	// });


	$(document).on('click', "#Add > a.btn", function(event) {
		event.preventDefault();
		$("#Add > ul").html("");
		$("th:not(:visible)").each(function () {
			$("#Add > ul").append("<li id='"+ $(this).attr("class")+"'><a href=\"#\">"+
                            ($(this).html().replace('<a class="close">×</a>', ''))
                             +"</a></li>");
		});
	});

	$(document).on('click', "#Add > ul > li > a", function(event) {
		event.preventDefault();
    //$("."+$(this).text().split(" ").join(".")).detach().insertAfter($("th:visible:not(.add-button):last"));
		$("."+$(this).parent().attr("id").split(" ").join(".")).toggle();
    var visibile_count = $("th:visible:not(.add-button)").length;
    var pageInfo = currentPageAndPerPage();
    var bodyUrlPrefix;

    if ($("div[data-report-type]").attr("data-report-type") == "report") {
      bodyUrlPrefix = "/reports/";
    }
    else {
      bodyUrlPrefix = "/streams/"
    }

    $("table").css("background-color", "#FAFAFA");

    $.ajax({url: bodyUrlPrefix + "update-visibility/",
            type: "POST",
            data: {stream: $("div[data-stream]").attr("data-stream"), column_name: $(this).parent().attr("id"),
                   visibility: true, per: pageInfo.perPage, page: pageInfo.currentPage,
                   column_count: visibile_count}}).done(function(ret) {
      $("div[data-stream]").html(ret);
      $("th:visible:not(.add-button)").slice(visibile_count, $("th:visible:not(.add-button)").length).each(function() {
				$("."+$(this).attr("class")).toggle();
			});
      $("table").css("background-color", "transparent");
      callPagination();
    });
	});
});








