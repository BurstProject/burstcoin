var NRS = (function(NRS, $, undefined) {
	NRS.forms.errorMessages.startForging = {
		"5": "You cannot forge. Either your balance is 0 or your account is too new (you must wait a day or so)."
	};

	NRS.forms.startForgingComplete = function(response, data) {
		if ("deadline" in response) {
			$("#forging_indicator").addClass("forging");
			$("#forging_indicator span").html("Forging");
			NRS.isForging = true;
			$.growl("Forging started successfully.", {
				type: "success"
			});
		} else {
			NRS.isForging = false;
			$.growl("Couldn't start forging, unknown error.", {
				type: 'danger'
			});
		}
	}

	NRS.forms.stopForgingComplete = function(response, data) {
		if ($("#stop_forging_modal .show_logout").css("display") == "inline") {
			NRS.logout();
			return;
		}

		$("#forging_indicator").removeClass("forging");
		$("#forging_indicator span").html("Not forging");

		NRS.isForging = false;

		if (response.foundAndStopped) {
			$.growl("Forging stopped successfully.", {
				type: 'success'
			});
		} else {
			$.growl("You weren't forging to begin with.", {
				type: 'danger'
			});
		}
	}

	$("#forging_indicator").click(function(e) {
		e.preventDefault();

		if (NRS.downloadingBlockchain) {
			$.growl("The blockchain is busy downloading, you cannot forge during this time. Please try again when the blockchain is fully synced.", {
				"type": "danger"
			});
		} else if (NRS.state.isScanning) {
			$.growl("The blockchain is currently being rescanned, you cannot forge during this time. Please try again in a minute.", {
				"type": "danger"
			});
		} else if (!NRS.accountInfo.publicKey) {
			$.growl("You cannot forge because your account has no public key. Please make an outgoing transaction first.", {
				"type": "danger"
			});
		} else if (NRS.accountInfo.effectiveBalanceNXT == 0) {
			if (NRS.lastBlockHeight >= NRS.accountInfo.currentLeasingHeightFrom && NRS.lastBlockHeight <= NRS.accountInfo.currentLeasingHeightTo) {
				$.growl("Your effective balance is leased out, you cannot forge at the moment.", {
					"type": "danger"
				});
			} else {
				$.growl("Your effective balance is zero, you cannot forge.", {
					"type": "danger"
				});
			}
		} else if ($(this).hasClass("forging")) {
			$("#stop_forging_modal").modal("show");
		} else {
			$("#start_forging_modal").modal("show");
		}
	});

	return NRS;
}(NRS || {}, jQuery));