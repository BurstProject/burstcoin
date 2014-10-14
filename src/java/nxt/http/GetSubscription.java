package nxt.http;

import nxt.Account;
import nxt.NxtException;
import nxt.Subscription;
import nxt.util.Convert;

import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

import javax.servlet.http.HttpServletRequest;

public final class GetSubscription extends APIServlet.APIRequestHandler {
	
	static final GetSubscription instance = new GetSubscription();
	
	private GetSubscription() {
		super(new APITag[] {APITag.ACCOUNTS}, "subscription");
	}
	
	@Override
	JSONStreamAware processRequest(HttpServletRequest req) throws NxtException {
		Long subscriptionId;
		try {
			subscriptionId = Convert.parseUnsignedLong(Convert.emptyToNull(req.getParameter("subscription")));
		}
		catch(Exception e) {
			JSONObject response = new JSONObject();
			response.put("errorCode", 3);
			response.put("errorDescription", "Invalid or not specified subscription");
			return response;
		}
		
		Subscription subscription = Subscription.getSubscription(subscriptionId);
		if(subscription == null) {
			JSONObject response = new JSONObject();
			response.put("errorCode", 5);
			response.put("errorDescription", "Subscription not found");
			return response;
		}
		
		return JSONData.subscription(subscription);
	}
}
