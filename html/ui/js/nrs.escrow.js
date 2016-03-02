/**
 * @depends {nrs.js}
 */
 var NRS = (function(NRS, $, undefined) {
	NRS.escrowPageType = null;

	NRS.pages.escrow = function() {
		NRS.sendRequest("getAccountEscrowTransactions", {
			"account": NRS.account
		}, function(response) {
			if(response.escrows && response.escrows.length) {
				var escrows = {};
				var rows = "";
				
				for(var i = 0; i < response.escrows.length; i++) {
					rows += "<tr><td><a href='#' data-escrow='" + String(response.escrows[i].id).escapeHTML() + "'>" + String(response.escrows[i].id).escapeHTML() + "</a></td><td>" + String(response.escrows[i].senderRS).escapeHTML() + "</td><td>" + String(response.escrows[i].recipientRS).escapeHTML() + "</td><td>" + NRS.formatAmount(response.escrows[i].amountNQT) + "</td</tr>";
				}
			}
			NRS.dataLoaded(rows);
		});
	}

	return NRS;
}(NRS || {}, jQuery));