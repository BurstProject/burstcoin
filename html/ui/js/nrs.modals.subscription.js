/**
 * @depends {nrs.js}
 * @depends {nrs.modals.js}
 */
var NRS = (function(NRS, $, undefined) {
	$("#subscription_table").on("click", "a[data-subscription]", function(e) {
		e.preventDefault();

		var subscriptionId = $(this).data("subscription");

		NRS.showSubscriptionCancelModal(subscriptionId);
	});
	
	NRS.showSubscriptionCancelModal = function(subscription) {
		if (NRS.fetchingModalData) {
			return;
		}
		
		NRS.fetchingModalData = true;
		
		if(typeof subscription != "object") {
			NRS.sendRequest("getSubscription", {
				"subscription": subscription
			}, function(response, input) {
				NRS.processSubscriptionCancelModalData(response);
			});
		}
		else {
			NRS.processSubscriptionCancelModalData(subscription);
		}
	};
	
	NRS.processSubscriptionCancelModalData = function(subscription) {
		
		$("#subscription_cancel_subscription").val(subscription.id);
		$("#subscription_cancel_sender").html(subscription.senderRS);
		$("#subscription_cancel_recipient").html(subscription.recipientRS);
		$("#subscription_cancel_amount").html(NRS.formatAmount(subscription.amountNQT));
		$("#subscription_cancel_frequency").html(subscription.frequency);
		$("#subscription_cancel_time_next").html(NRS.formatTimestamp(subscription.timeNext));
		
		$("#subscription_cancel_modal").modal("show");
		NRS.fetchingModalData = false;
	};

	return NRS;
}(NRS || {}, jQuery));