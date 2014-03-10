
$( document ).ready( function() {
	$("div[data-stream]").html("<center><div class='spinner'><div class='dot1'></div><div class='dot2'></div></div></center>");

	var str_name = $("div[data-stream]").attr("data-stream");
  var report_type = $("div[data-report-type]").attr("data-report-type");
	var manip_fn = $("div[data-manip-fn]").attr("data-manip-fn");
	var body_url = "/streams/data-body/"+str_name;
	var req_data = "";

  if (report_type == "report") {
      body_url = "/reports/body/"+str_name;
  }

	// if (manip_fn != "render") {
		body_url += location.search;
	// }
	// else {
	// 	req_data = {stream: str_name, fn: manip_fn}
	// }
	//alert(body_url);

	$.ajax({
		type: "GET",
		url: body_url
		// data: req_data
		}).done(function(ret) {

			$("div[data-stream]").html(ret);
			$("th:visible:not(.add-button)").slice($("table[data-visible-columns]").attr("data-visible-columns"), $("th:visible:not(.add-button)").length).each(function() {
				$("."+$(this).attr("class")).toggle();
			});
      callPagination();
		});

});





