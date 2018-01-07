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

  static final String TRANSACTION_BYTES_PARAMETER_FIELD = "transactionBytes";
  static final String TRANSACTION_JSON_PARAMETER_FIELD = "transactionJSON";

  static final String TRANSACTION_RESPONSE_FIELD = "transaction";
  static final String FULL_HASH_RESPONSE_FIELD = "fullHash";
  static final String ERROR_CODE_RESPONSE_FIELD = "errorCode";
  static final String ERROR_DESCRIPTION_RESPONSE_FIELD = "errorDescription";
  static final String ERROR_RESPONSE_FIELD = "error";

  private BroadcastTransaction() {
    super(new APITag[]{APITag.TRANSACTIONS}, TRANSACTION_BYTES_PARAMETER_FIELD, TRANSACTION_JSON_PARAMETER_FIELD);
  }

  @Override
  JSONStreamAware processRequest(HttpServletRequest req) throws BurstException {

    String transactionBytes = Convert.emptyToNull(req.getParameter(TRANSACTION_BYTES_PARAMETER_FIELD));
    String transactionJSON = Convert.emptyToNull(req.getParameter(TRANSACTION_JSON_PARAMETER_FIELD));
    Transaction transaction = ParameterParser.parseTransaction(transactionBytes, transactionJSON);
    JSONObject response = new JSONObject();
    try {
      transaction.validate();
      DIContainer.getTransactionProcessor().broadcast(transaction);
      response.put(TRANSACTION_RESPONSE_FIELD, transaction.getStringId());
      response.put(FULL_HASH_RESPONSE_FIELD, transaction.getFullHash());
    } catch (BurstException.ValidationException | RuntimeException e) {
      logger.log(Level.INFO, e.getMessage(), e);
      response.put(ERROR_CODE_RESPONSE_FIELD, 4);
      response.put(ERROR_DESCRIPTION_RESPONSE_FIELD, "Incorrect transaction: " + e.toString());
      response.put(ERROR_RESPONSE_FIELD, e.getMessage());
    }
    return response;

  }

  @Override
  boolean requirePost() {
    return true;
  }

}
