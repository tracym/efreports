$( document ).ready( function() {
	$("div[data-stream]").html("<center><i class=\"icon-spinner icon-spin\"></i> Mapping Data...</center>");

	var from_str_name = $("div[data-stream]").attr("data-stream");
	var to_str_name = $("div[data-stream]").attr("data-to-stream");

	$.ajax({
		type: "POST",
		url: "/streams/map-data-body",
		data: {from_stream: from_str_name, to_stream: to_str_name}
		}).done(function(ret) {
			$("div[data-stream]").html(ret);
			$("th:visible").slice(6, $("th:visible").length).each(function() {
				$("."+$(this).attr("class")).toggle();
			});
		});

});