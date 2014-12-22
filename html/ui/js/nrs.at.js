/**
 * @depends {nrs.js}
 */
 var NRS = (function(NRS, $, undefined) {
	NRS.atPageType = null;

	NRS.pages.at = function() {
		NRS.sendRequest("getAccountATs", {
			"account": NRS.account
		}, function(response) {
			if(response.ats && response.ats.length) {
				var ats = {};
				var rows = "";
				
				for(var i = 0; i < response.ats.length; i++) {
					rows += "<tr><td>" + String(response.ats[i].atRS).escapeHTML() + "</td><td>" + String(response.ats[i].name).escapeHTML() + "</td><td>" + String(response.ats[i].description).escapeHTML() + "</td><td>" + NRS.formatAmount(response.ats[i].balanceNQT) + "</td></tr>";
				}
			}
			NRS.dataLoaded(rows);
		});
	}

	return NRS;
}(NRS || {}, jQuery));