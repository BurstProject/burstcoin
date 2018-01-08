package brs.http;

import brs.TransactionProcessor;
import brs.TransactionProcessorImpl;
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
