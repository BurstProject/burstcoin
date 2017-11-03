package brs.http;

import brs.Burst;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

import javax.servlet.http.HttpServletRequest;

public final class FullReset extends APIServlet.APIRequestHandler {

  static final FullReset instance = new FullReset();

  private FullReset() {
    super(new APITag[] {APITag.DEBUG});
  }

  @Override
  JSONStreamAware processRequest(HttpServletRequest req) {
    JSONObject response = new JSONObject();
    try {
      Burst.getBlockchainProcessor().fullReset();
      response.put("done", true);
    } catch (RuntimeException e) {
      response.put("error", e.toString());
    }
    return response;
  }

  @Override
  final boolean requirePost() {
    return true;
  }

}
