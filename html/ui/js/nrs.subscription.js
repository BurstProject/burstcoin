/**
 * @depends {nrs.js}
 */
 var NRS = (function(NRS, $, undefined) {
	NRS.subscriptionPageType = null;

	NRS.pages.subscription = function() {
		NRS.sendRequest("getAccountSubscriptions", {
			"account": NRS.account
		}, function(response) {
			if(response.subscriptions && response.subscriptions.length) {
				var subscriptions = {};
				var rows = "";
				
				for(var i = 0; i < response.subscriptions.length; i++) {
					rows += "<tr><td><a href='#' data-subscription='" + String(response.subscriptions[i].id).escapeHTML() + "'>" + String(response.subscriptions[i].id).escapeHTML() + "</a></td><td>" + String(response.subscriptions[i].senderRS).escapeHTML() + "</td><td>" + String(response.subscriptions[i].recipientRS).escapeHTML() + "</td><td>" + NRS.formatAmount(response.subscriptions[i].amountNQT) + "</td><td>" + response.subscriptions[i].frequency + "</td><td>" + NRS.formatTimestamp(response.subscriptions[i].timeNext) + "</td></tr>";
				}
			}
			NRS.dataLoaded(rows);
		});
	}

	return NRS;
}(NRS || {}, jQuery));