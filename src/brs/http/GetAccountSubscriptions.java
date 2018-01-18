package brs.http;

import brs.Account;
import brs.BurstException;
import brs.Subscription;
import brs.db.BurstIterator;
import brs.services.ParameterService;
import javax.servlet.http.HttpServletRequest;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

import static brs.http.common.Parameters.ACCOUNT_PARAMETER;

public final class GetAccountSubscriptions extends APIServlet.APIRequestHandler {

  private final ParameterService parameterService;

  GetAccountSubscriptions(ParameterService parameterService) {
    super(new APITag[]{APITag.ACCOUNTS}, ACCOUNT_PARAMETER);
    this.parameterService = parameterService;
  }

  @Override
  JSONStreamAware processRequest(HttpServletRequest req) throws BurstException {

    Account account = parameterService.getAccount(req);

    JSONObject response = new JSONObject();

    JSONArray subscriptions = new JSONArray();

    BurstIterator<Subscription> accountSubscriptions = Subscription.getSubscriptionsByParticipant(account.getId());

    while (accountSubscriptions.hasNext()) {
      subscriptions.add(JSONData.subscription(accountSubscriptions.next()));
    }

    response.put("subscriptions", subscriptions);
    return response;
  }
}
