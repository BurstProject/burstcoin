/**
 * @depends {nrs.js}
 * @depends {nrs.modals.js}
 */
var NRS = (function(NRS, $, undefined) {
	$("#account_info_modal").on("show.bs.modal", function(e) {
		$("#account_info_name").val(NRS.accountInfo.name);
		$("#account_info_description").val(NRS.accountInfo.description);
	});

	NRS.forms.setAccountInfoComplete = function(response, data) {
		var name = $.trim(String(data.name));
		if (name) {
			$("#account_name").html(name.escapeHTML()).removeAttr("data-i18n");
		} else {
			$("#account_name").html($.t("no_name_set")).attr("data-i18n", "no_name_set");
		}

		var description = $.trim(String(data.description));

		setTimeout(function() {
			NRS.accountInfo.description = description;
			NRS.accountInfo.name = name;
		}, 1000);
	}

	return NRS;
}(NRS || {}, jQuery));