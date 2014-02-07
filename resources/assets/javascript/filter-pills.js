function filterKeyList () {
  var ret = "";
  $("li.filter-pill:visible").each(function(){
      ret += "filter-key-"+$(this).attr("id").replace(":", "") +"="+$(this).attr("data-filter-val")+"&"+"filter-op-"+$(this).attr("id").replace(":", "") + "="+$(this).attr("data-filter-op");
    });
  return ret;
}


$( document ).ready( function() {
  $(document).on('click', "li.filter-pill > a", function() {
		$(this).parent().toggle();
    $("table").css("background-color", "#FAFAFA");

    var streamName = streamNameFromPath();
    var pageInfo = currentPageAndPerPage();
    var currentState = history.state;
    history.pushState(currentState, "", streamName + "?page=1&per="+pageInfo.perPage);

    $.ajax({
       url: location.pathname.replace("data-header", "data-body") + "?fn=filter-map-replace&"+filterKeyList()
      //url: location.pathname + "?fn=filter-map-replace&"+filterKeyList()

    }).done(function(ret) {

			$("div[data-stream]").html(ret);
			$("th:visible").slice($("table[data-visible-columns]").attr("data-visible-columns"), $("th:visible:not(.add-button)").length).each(function() {
				$("."+$(this).attr("class")).toggle();
			});
      $("table").css("background-color", "transparent");
      callPagination();
	});
 });
});



