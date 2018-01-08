package brs.http;

import brs.Account;
import brs.BurstException;
import brs.Subscription;
import brs.db.BurstIterator;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

import javax.servlet.http.HttpServletRequest;

public final class GetAccountSubscriptions extends APIServlet.APIRequestHandler {
	
  static final GetAccountSubscriptions instance = new GetAccountSubscriptions();
	
  private GetAccountSubscriptions() {
    super(new APITag[] {APITag.ACCOUNTS}, "account");
  }
	
  @Override
  JSONStreamAware processRequest(HttpServletRequest req) throws BurstException {
		
    Account account = ParameterParser.getAccount(req);
		
    JSONObject response = new JSONObject();
		
    JSONArray subscriptions = new JSONArray();

    BurstIterator<Subscription> accountSubscriptions = Subscription.getSubscriptionsByParticipant(account.getId());
		
    while(accountSubscriptions.hasNext()) {
      subscriptions.add(JSONData.subscription(accountSubscriptions.next()));
    }
		
    response.put("subscriptions", subscriptions);
    return response;
  }
}
