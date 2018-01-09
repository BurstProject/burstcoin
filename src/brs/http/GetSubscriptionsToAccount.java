package brs.http;

import static brs.http.common.Parameters.ACCOUNT_PARAMETER;

import brs.Account;
import brs.BurstException;
import brs.Subscription;
import brs.db.BurstIterator;
import brs.services.ParameterService;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

import javax.servlet.http.HttpServletRequest;

public final class GetSubscriptionsToAccount extends APIServlet.APIRequestHandler {

  private final ParameterService parameterService;

  GetSubscriptionsToAccount(ParameterService parameterService) {
    super(new APITag[] {APITag.ACCOUNTS}, ACCOUNT_PARAMETER);
    this.parameterService = parameterService;
  }
	
  @Override
  JSONStreamAware processRequest(HttpServletRequest req) throws BurstException {
    final Account account = parameterService.getAccount(req);
		
    JSONObject response = new JSONObject();
		
    JSONArray subscriptions = new JSONArray();

    BurstIterator<Subscription> accountSubscriptions = Subscription.getSubscriptionsToId(account.getId());
		
    while(accountSubscriptions.hasNext()) {
      subscriptions.add(JSONData.subscription(accountSubscriptions.next()));
    }
		
    response.put("subscriptions", subscriptions);
    return response;
  }
}
