package nxt.http;

import java.util.Collection;

import nxt.Account;
import nxt.NxtException;
import nxt.Subscription;
import nxt.db.DbIterator;
import nxt.util.Convert;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

import javax.servlet.http.HttpServletRequest;

public final class GetSubscriptionsToAccount extends APIServlet.APIRequestHandler {
	
	static final GetSubscriptionsToAccount instance = new GetSubscriptionsToAccount();
	
	private GetSubscriptionsToAccount() {
		super(new APITag[] {APITag.ACCOUNTS}, "account");
	}
	
	@Override
	JSONStreamAware processRequest(HttpServletRequest req) throws NxtException {
		
		Account account = ParameterParser.getAccount(req);
		
		JSONObject response = new JSONObject();
		
		JSONArray subscriptions = new JSONArray();
		
		DbIterator<Subscription> accountSubscriptions = Subscription.getSubscriptionsToId(account.getId());
		
		for(Subscription subscription : accountSubscriptions) {
			subscriptions.add(JSONData.subscription(subscription));
		}
		
		response.put("subscriptions", subscriptions);
		return response;
	}
}
