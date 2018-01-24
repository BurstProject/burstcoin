package brs.http;

import static brs.http.common.Parameters.ACCOUNT_PARAMETER;

import brs.Account;
import brs.BurstException;
import brs.Subscription;
import brs.db.BurstIterator;
import brs.services.ParameterService;
import brs.services.SubscriptionService;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

import javax.servlet.http.HttpServletRequest;

public final class GetSubscriptionsToAccount extends APIServlet.APIRequestHandler {

  private final ParameterService parameterService;
  private final SubscriptionService subscriptionService;

  GetSubscriptionsToAccount(ParameterService parameterService, SubscriptionService subscriptionService) {
    super(new APITag[] {APITag.ACCOUNTS}, ACCOUNT_PARAMETER);
    this.parameterService = parameterService;
    this.subscriptionService = subscriptionService;
  }
	
  @Override
  JSONStreamAware processRequest(HttpServletRequest req) throws BurstException {
    final Account account = parameterService.getAccount(req);
		
    JSONObject response = new JSONObject();
		
    JSONArray subscriptions = new JSONArray();

    BurstIterator<Subscription> accountSubscriptions = subscriptionService.getSubscriptionsToId(account.getId());
		
    while(accountSubscriptions.hasNext()) {
      subscriptions.add(JSONData.subscription(accountSubscriptions.next()));
    }
		
    response.put("subscriptions", subscriptions);
    return response;
  }
}
