package brs.http;

import static brs.http.common.ResultFields.DONE_RESPONSE;
import static brs.http.common.ResultFields.ERROR_RESPONSE;

import brs.TransactionProcessor;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

import javax.servlet.http.HttpServletRequest;

public final class ClearUnconfirmedTransactions extends APIServlet.APIRequestHandler {

  private final TransactionProcessor transactionProcessor;

  ClearUnconfirmedTransactions(TransactionProcessor transactionProcessor) {
    super(new APITag[] {APITag.DEBUG});
    this.transactionProcessor = transactionProcessor;
  }

  @Override
  JSONStreamAware processRequest(HttpServletRequest req) {
    JSONObject response = new JSONObject();
    try {
      transactionProcessor.clearUnconfirmedTransactions();
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
