package brs.http;

import brs.Account;
import brs.Burst;
import brs.BurstException;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

import javax.servlet.http.HttpServletRequest;

public final class GetAccountLessors extends APIServlet.APIRequestHandler {

  static final GetAccountLessors instance = new GetAccountLessors();

  private GetAccountLessors() {
    super(new APITag[] {APITag.ACCOUNTS}, "account", "height");
  }

  @Override
  JSONStreamAware processRequest(HttpServletRequest req) throws BurstException {

    Account account = ParameterParser.getAccount(req);
    int height = ParameterParser.getHeight(req);
    if (height < 0) {
      height = Burst.getBlockchain().getHeight();
    }

    JSONObject response = new JSONObject();
    JSONData.putAccount(response, "account", account.getId());
    response.put("height", height);
    JSONArray lessorsJSON = new JSONArray();

    /*try (DbIterator<Account> lessors = account.getLessors(height)) {
      if (lessors.hasNext()) {
      while (lessors.hasNext()) {
      Account lessor = lessors.next();
      JSONObject lessorJSON = new JSONObject();
      JSONData.putAccount(lessorJSON, "lessor", lessor.getId());
      lessorJSON.put("guaranteedBalanceNQT", String.valueOf(account.getGuaranteedBalanceNQT(1440, height)));
      lessorsJSON.add(lessorJSON);
      }
      }
      }*/
    response.put("lessors", lessorsJSON);
    return response;

  }

}
