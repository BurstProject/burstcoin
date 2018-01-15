package brs.http;

import brs.BurstException;
import brs.Subscription;
import brs.util.Convert;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

import javax.servlet.http.HttpServletRequest;

import static brs.http.common.Parameters.SUBSCRIPTION_PARAMETER;
import static brs.http.common.ResultFields.ERROR_CODE_RESPONSE;
import static brs.http.common.ResultFields.ERROR_DESCRIPTION_RESPONSE;

public final class GetSubscription extends APIServlet.APIRequestHandler {
	
  static final GetSubscription instance = new GetSubscription();
	
  private GetSubscription() {
    super(new APITag[] {APITag.ACCOUNTS}, SUBSCRIPTION_PARAMETER);
  }
	
  @Override
  JSONStreamAware processRequest(HttpServletRequest req) throws BurstException {
    Long subscriptionId;
    try {
      subscriptionId = Convert.parseUnsignedLong(Convert.emptyToNull(req.getParameter(SUBSCRIPTION_PARAMETER)));
    }
    catch(Exception e) {
      JSONObject response = new JSONObject();
      response.put(ERROR_CODE_RESPONSE, 3);
      response.put(ERROR_DESCRIPTION_RESPONSE, "Invalid or not specified subscription");
      return response;
    }
		
    Subscription subscription = Subscription.getSubscription(subscriptionId);
    if(subscription == null) {
      JSONObject response = new JSONObject();
      response.put(ERROR_CODE_RESPONSE, 5);
      response.put(ERROR_DESCRIPTION_RESPONSE, "Subscription not found");
      return response;
    }
		
    return JSONData.subscription(subscription);
  }
}
