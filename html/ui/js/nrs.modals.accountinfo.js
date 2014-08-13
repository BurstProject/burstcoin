var NRS = (function(NRS, $, undefined) {
	$("#account_info_modal").on("show.bs.modal", function(e) {
		$("#account_info_name").val(NRS.accountInfo.name);
		$("#account_info_description").val(NRS.accountInfo.description);
	});

	NRS.forms.setAccountInfoComplete = function(response, data) {
		var name = $.trim(String(data.name));
		if (name) {
			$("#account_name").html(name.escapeHTML());
		} else {
			$("#account_name").html("No name set");
		}
	}

	return NRS;
}(NRS || {}, jQuery));