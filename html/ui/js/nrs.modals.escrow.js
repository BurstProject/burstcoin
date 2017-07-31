/**
 * @depends {nrs.js}
 * @depends {nrs.modals.js}
 */
var NRS = (function(NRS, $, undefined) {
	$("#escrow_table").on("click", "a[data-escrow]", function(e) {
		e.preventDefault();

		var escrowId = $(this).data("escrow");

		NRS.showEscrowDecisionModal(escrowId);
	});
	
	NRS.showEscrowDecisionModal = function(escrow) {
		if (NRS.fetchingModalData) {
			return;
		}
		
		NRS.fetchingModalData = true;
		
		if(typeof escrow != "object") {
			NRS.sendRequest("getEscrowTransaction", {
				"escrow": escrow
			}, function(response, input) {
				NRS.processEscrowDecisionModalData(response);
			});
		}
		else {
			NRS.processEscrowDecisionModalData(escrow);
		}
	};
	
	NRS.processEscrowDecisionModalData = function(escrow) {
		
		$("#escrow_decision_escrow").val(escrow.id);
		var decisions = "";
		for(var i = 0; i < escrow.signers.length; i++) {
			decisions += escrow.signers[i].idRS + " " + escrow.signers[i].decision + "<br />";
		}
		$("#escrow_decision_decisions").html(decisions);
		$("#escrow_decision_required").html(escrow.requiredSigners + " signers required");
		$("#escrow_decision_deadline").html("Defaults to " + escrow.deadlineAction + " at " + NRS.formatTimestamp(escrow.deadline));
		
		$("#escrow_decision_modal").modal("show");
		NRS.fetchingModalData = false;
	};

	return NRS;
}(NRS || {}, jQuery));