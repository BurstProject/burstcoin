package nxt.http;

import javax.servlet.http.HttpServletRequest;

import nxt.Account;
import nxt.Attachment;
import nxt.Constants;
import nxt.NxtException;
import nxt.Subscription;
import nxt.util.Convert;

import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

public final class SubscriptionCancel extends CreateTransaction {
	
	static SubscriptionCancel instance = new SubscriptionCancel();
	
	private SubscriptionCancel() {
		super(new APITag[] {APITag.TRANSACTIONS, APITag.CREATE_TRANSACTION},
			  "subscription");
	}
	
	@Override
	JSONStreamAware processRequest(HttpServletRequest req) throws NxtException {
		Account sender = ParameterParser.getSenderAccount(req);
		
		String subscriptionString = Convert.emptyToNull(req.getParameter("subscription"));
		if(subscriptionString == null) {
			JSONObject response = new JSONObject();
			response.put("errorCode", 3);
			response.put("errorDescription", "Subscription Id not specified");
			return response;
		}
		
		Long subscriptionId;
		try {
			subscriptionId = Convert.parseUnsignedLong(subscriptionString);
		}
		catch(Exception e) {
			JSONObject response = new JSONObject();
			response.put("errorCode", 4);
			response.put("errorDescription", "Failed to parse subscription id");
			return response;
		}
		
		Subscription subscription = Subscription.getSubscription(subscriptionId);
		if(subscription == null) {
			JSONObject response = new JSONObject();
			response.put("errorCode", 5);
			response.put("errorDescription", "Subscription not found");
			return response;
		}
		
		if(sender.getId() != subscription.getSenderId() &&
		   sender.getId()!= subscription.getRecipientId()) {
			JSONObject response = new JSONObject();
			response.put("errorCode", 7);
			response.put("errorDescription", "Must be sender or recipient to cancel subscription");
			return response;
		}
		
		Attachment.AdvancedPaymentSubscriptionCancel attachment = new Attachment.AdvancedPaymentSubscriptionCancel(subscription.getId());
		
		return createTransaction(req, sender, null, 0, attachment);
	}
}
