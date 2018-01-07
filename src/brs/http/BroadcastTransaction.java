package brs.http;

import brs.BurstException;
import brs.DIContainer;
import brs.Transaction;
import brs.util.Convert;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.servlet.http.HttpServletRequest;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

public final class BroadcastTransaction extends APIServlet.APIRequestHandler {

  private static final Logger logger = Logger.getLogger(BroadcastTransaction.class.getSimpleName());

  static final BroadcastTransaction instance = new BroadcastTransaction();

  static final String TRANSACTION_BYTES_PARAMETER = "transactionBytes";
  static final String TRANSACTION_JSON_PARAMETER = "transactionJSON";

  static final String TRANSACTION_RESPONSE = "transaction";
  static final String FULL_HASH_RESPONSE = "fullHash";
  static final String ERROR_CODE_RESPONSE = "errorCode";
  static final String ERROR_DESCRIPTION_RESPONSE = "errorDescription";
  static final String ERROR_RESPONSE = "error";

  private BroadcastTransaction() {
    super(new APITag[]{APITag.TRANSACTIONS}, TRANSACTION_BYTES_PARAMETER, TRANSACTION_JSON_PARAMETER);
  }

  @Override
  JSONStreamAware processRequest(HttpServletRequest req) throws BurstException {

    String transactionBytes = Convert.emptyToNull(req.getParameter(TRANSACTION_BYTES_PARAMETER));
    String transactionJSON = Convert.emptyToNull(req.getParameter(TRANSACTION_JSON_PARAMETER));
    Transaction transaction = ParameterParser.parseTransaction(transactionBytes, transactionJSON);
    JSONObject response = new JSONObject();
    try {
      transaction.validate();
      DIContainer.getTransactionProcessor().broadcast(transaction);
      response.put(TRANSACTION_RESPONSE, transaction.getStringId());
      response.put(FULL_HASH_RESPONSE, transaction.getFullHash());
    } catch (BurstException.ValidationException | RuntimeException e) {
      logger.log(Level.INFO, e.getMessage(), e);
      response.put(ERROR_CODE_RESPONSE, 4);
      response.put(ERROR_DESCRIPTION_RESPONSE, "Incorrect transaction: " + e.toString());
      response.put(ERROR_RESPONSE, e.getMessage());
    }
    return response;

  }

  @Override
  boolean requirePost() {
    return true;
  }

}
