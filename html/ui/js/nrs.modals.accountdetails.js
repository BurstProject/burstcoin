var NRS = (function(NRS, $, undefined) {
	$("#account_details_modal").on("show.bs.modal", function(e) {
		$("#account_details_modal_balance").show();

		if (NRS.accountInfo.errorCode) {
			$("#account_balance_table").hide();

			if (NRS.accountInfo.errorCode == 5) {
				$("#account_balance_warning").html("Your account is brand new. You should fund it with some coins. Your account ID is <strong>" + NRS.account + "</strong>").show();
			} else {
				$("#account_balance_warning").html(NRS.accountInfo.errorDescription.escapeHTML()).show();
			}
		} else {
			$("#account_balance_warning").hide();

			$("#account_balance_balance").html(NRS.formatAmount(new BigInteger(NRS.accountInfo.balanceNQT)) + " BURST");
			$("#account_balance_unconfirmed_balance").html(NRS.formatAmount(new BigInteger(NRS.accountInfo.unconfirmedBalanceNQT)) + " BURST");
			$("#account_balance_effective_balance").html(NRS.formatAmount(NRS.accountInfo.effectiveBalanceNXT) + " BURST");
			$("#account_balance_guaranteed_balance").html(NRS.formatAmount(new BigInteger(NRS.accountInfo.guaranteedBalanceNQT)) + " BURST");

			$("#account_balance_public_key").html(String(NRS.accountInfo.publicKey).escapeHTML());
			$("#account_balance_account_id").html(String(NRS.account).escapeHTML());
			$("#account_balance_account_rs").html(String(NRS.accountInfo.accountRS).escapeHTML());

			if (!NRS.accountInfo.publicKey) {
				$("#account_balance_public_key").html("/");
				$("#account_balance_warning").html("Your account does not have a public key! This means it's not as protected as other accounts. You must make an outgoing transaction to fix this issue. (<a href='#' data-toggle='modal' data-target='#send_message_modal'>send a message</a>, <a href='#' data-toggle='modal' data-target='#register_alias_modal'>buy an alias</a>, <a href='#' data-toggle='modal' data-target='#send_money_modal'>send Burst</a>, ...)").show();
			}
		}
	});

	$("#account_details_modal ul.nav li").click(function(e) {
		e.preventDefault();

		var tab = $(this).data("tab");

		$(this).siblings().removeClass("active");
		$(this).addClass("active");

		$(".account_details_modal_content").hide();

		var content = $("#account_details_modal_" + tab);

		content.show();
	});

	$("#account_details_modal").on("hidden.bs.modal", function(e) {
		$(this).find(".account_details_modal_content").hide();
		$(this).find("ul.nav li.active").removeClass("active");
		$("#account_details_balance_nav").addClass("active");
	});

	return NRS;
}(NRS || {}, jQuery));
