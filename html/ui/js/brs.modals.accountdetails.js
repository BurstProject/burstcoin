/**
 * @depends {brs.js}
 * @depends {brs.modals.js}
 */
var BRS = (function(BRS, $, undefined) {
    $("#account_details_modal").on("show.bs.modal", function(e) {
	$("#account_details_modal_qr_code").empty().qrcode({
	    "text": BRS.accountRS,
	    "width": 128,
	    "height": 128
	});

	$("#account_details_modal_balance").show();

	if (BRS.accountInfo.errorCode && BRS.accountInfo.errorCode != 5) {
	    $("#account_balance_table").hide();
	    //todo
	    $("#account_balance_warning").html(String(BRS.accountInfo.errorDescription).escapeHTML()).show();
	}
        else {
	    $("#account_balance_warning").hide();

	    if (BRS.accountInfo.errorCode && BRS.accountInfo.errorCode == 5) {
		$("#account_balance_balance, #account_balance_unconfirmed_balance, #account_balance_effective_balance, #account_balance_guaranteed_balance").html("0 BURST");
		$("#account_balance_public_key").html(String(BRS.publicKey).escapeHTML());
		$("#account_balance_account_rs").html(String(BRS.accountRS).escapeHTML());
		$("#account_balance_account").html(String(BRS.account).escapeHTML());
	    }
            else {
		$("#account_balance_balance").html(BRS.formatAmount(new BigInteger(BRS.accountInfo.balanceNQT)) + " BURST");
		$("#account_balance_unconfirmed_balance").html(BRS.formatAmount(new BigInteger(BRS.accountInfo.unconfirmedBalanceNQT)) + " BURST");
		$("#account_balance_effective_balance").html(BRS.formatAmount(BRS.accountInfo.effectiveBalanceBURST) + " BURST");
		$("#account_balance_guaranteed_balance").html(BRS.formatAmount(new BigInteger(BRS.accountInfo.guaranteedBalanceNQT)) + " BURST");

		$("#account_balance_public_key").html(String(BRS.accountInfo.publicKey).escapeHTML());
		$("#account_balance_account_rs").html(String(BRS.accountInfo.accountRS).escapeHTML());
		$("#account_balance_account").html(String(BRS.account).escapeHTML());

		if (!BRS.accountInfo.publicKey) {
		    $("#account_balance_public_key").html("/");
		    $("#account_balance_warning").html($.t("no_public_key_warning") + " " + $.t("public_key_actions")).show();
		}
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
	$("#account_details_modal_qr_code").empty();
    });

    return BRS;
}(BRS || {}, jQuery));
