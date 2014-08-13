var NRS = (function(NRS, $, undefined) {
	$("#generate_token_modal").on("show.bs.modal", function(e) {
		$("#generate_token_website").val("http://");
		$("#generate_token_token").html("").hide();
	});

	NRS.forms.generateToken = function($modal) {
		var url = $.trim($("#generate_token_website").val());

		if (!url || url == "http://") {
			return {
				"error": "Website is a required field."
			};
			$("#generate_token_token").html("").hide();
		} else {
			return {};
		}
	}

	NRS.forms.generateTokenComplete = function(response, data) {
		$("#generate_token_modal").find(".error_message").hide();

		if (response.token) {
			$("#generate_token_token").html("The generated token for <strong>" + data.website.escapeHTML() + "</strong> is: <br /><br /><textarea style='width:100%' rows='3'>" + response.token.escapeHTML() + "</textarea>").show();
		} else {
			$.growl("Could not generate token.", {
				"type": "danger"
			});
			$("#generate_token_modal").modal("hide");
		}
	}

	return NRS;
}(NRS || {}, jQuery));