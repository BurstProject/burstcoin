package brs.http;

import static brs.http.common.Parameters.TRANSACTION_BYTES_PARAMETER;
import static brs.http.common.Parameters.TRANSACTION_JSON_PARAMETER;
import static brs.http.common.ResultFields.ERROR_CODE_RESPONSE;
import static brs.http.common.ResultFields.ERROR_DESCRIPTION_RESPONSE;
import static brs.http.common.ResultFields.ERROR_RESPONSE;
import static brs.http.common.ResultFields.VALIDATE_RESPONSE;
import static brs.http.common.ResultFields.VERIFY_RESPONSE;

import brs.BurstException;
import brs.Transaction;
import brs.services.ParameterService;
import brs.util.Convert;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.http.HttpServletRequest;

public final class ParseTransaction extends APIServlet.APIRequestHandler {

  private static final Logger logger = LoggerFactory.getLogger(ParseTransaction.class);

  private final ParameterService parameterService;

  ParseTransaction(ParameterService parameterService) {
    super(new APITag[] {APITag.TRANSACTIONS}, TRANSACTION_BYTES_PARAMETER, TRANSACTION_JSON_PARAMETER);
    this.parameterService = parameterService;
  }

  @Override
  JSONStreamAware processRequest(HttpServletRequest req) throws BurstException {

    String transactionBytes = Convert.emptyToNull(req.getParameter(TRANSACTION_BYTES_PARAMETER));
    String transactionJSON = Convert.emptyToNull(req.getParameter(TRANSACTION_JSON_PARAMETER));
    Transaction transaction = parameterService.parseTransaction(transactionBytes, transactionJSON);
    JSONObject response = JSONData.unconfirmedTransaction(transaction);
    try {
      transaction.validate();
    } catch (BurstException.ValidationException|RuntimeException e) {
      logger.debug(e.getMessage(), e);
      response.put(VALIDATE_RESPONSE, false);
      response.put(ERROR_CODE_RESPONSE, 4);
      response.put(ERROR_DESCRIPTION_RESPONSE, "Invalid transaction: " + e.toString());
      response.put(ERROR_RESPONSE, e.getMessage());
    }
    response.put(VERIFY_RESPONSE, transaction.verifySignature() && transaction.verifyPublicKey());
    return response;
  }

}
