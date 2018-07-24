package brs.http;

import brs.BlockchainProcessor;
import javax.servlet.http.HttpServletRequest;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

import static brs.http.common.ResultFields.DONE_RESPONSE;
import static brs.http.common.ResultFields.ERROR_RESPONSE;

public final class FullReset extends APIServlet.APIRequestHandler {

  private BlockchainProcessor blockchainProcessor;

  FullReset(BlockchainProcessor blockchainProcessor) {
    super(new APITag[]{APITag.DEBUG});
    this.blockchainProcessor = blockchainProcessor;
  }

  @Override
  JSONStreamAware processRequest(HttpServletRequest req) {
    JSONObject response = new JSONObject();
    try {
      blockchainProcessor.fullReset();
      response.put(DONE_RESPONSE, true);
    } catch (RuntimeException e) {
      response.put(ERROR_RESPONSE, e.toString());
    }
    return response;
  }

  @Override
  final boolean requirePost() {
    return true;
  }

}
