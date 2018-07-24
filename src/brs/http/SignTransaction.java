package brs.http;

import brs.BurstException;
import brs.Transaction;
import brs.crypto.Crypto;
import brs.services.ParameterService;
import brs.services.TransactionService;
import brs.util.Convert;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.http.HttpServletRequest;
import java.util.Arrays;

import static brs.http.JSONResponses.MISSING_SECRET_PHRASE;
import static brs.http.common.Parameters.SECRET_PHRASE_PARAMETER;
import static brs.http.common.Parameters.UNSIGNED_TRANSACTION_BYTES_PARAMETER;
import static brs.http.common.Parameters.UNSIGNED_TRANSACTION_JSON_PARAMETER;
import static brs.http.common.ResultFields.ERROR_CODE_RESPONSE;
import static brs.http.common.ResultFields.ERROR_DESCRIPTION_RESPONSE;
import static brs.http.common.ResultFields.ERROR_RESPONSE;
import static brs.http.common.ResultFields.FULL_HASH_RESPONSE;
import static brs.http.common.ResultFields.SIGNATURE_HASH_RESPONSE;
import static brs.http.common.ResultFields.TRANSACTION_BYTES_RESPONSE;
import static brs.http.common.ResultFields.TRANSACTION_RESPONSE;
import static brs.http.common.ResultFields.VERIFY_RESPONSE;

public final class SignTransaction extends APIServlet.APIRequestHandler {

  private static final Logger logger = LoggerFactory.getLogger(SignTransaction.class);

  private final ParameterService parameterService;
  private final TransactionService transactionService;

  SignTransaction(ParameterService parameterService, TransactionService transactionService) {
    super(new APITag[] {APITag.TRANSACTIONS}, UNSIGNED_TRANSACTION_BYTES_PARAMETER, UNSIGNED_TRANSACTION_JSON_PARAMETER, SECRET_PHRASE_PARAMETER);
    this.parameterService = parameterService;
    this.transactionService = transactionService;
  }

  @Override
  JSONStreamAware processRequest(HttpServletRequest req) throws BurstException {

    String transactionBytes = Convert.emptyToNull(req.getParameter(UNSIGNED_TRANSACTION_BYTES_PARAMETER));
    String transactionJSON = Convert.emptyToNull(req.getParameter(UNSIGNED_TRANSACTION_JSON_PARAMETER));
    Transaction transaction = parameterService.parseTransaction(transactionBytes, transactionJSON);

    String secretPhrase = Convert.emptyToNull(req.getParameter(SECRET_PHRASE_PARAMETER));
    if (secretPhrase == null) {
      return MISSING_SECRET_PHRASE;
    }

    JSONObject response = new JSONObject();
    try {
      transactionService.validate(transaction);
      if (transaction.getSignature() != null) {
        response.put(ERROR_CODE_RESPONSE, 4);
        response.put(ERROR_DESCRIPTION_RESPONSE, "Incorrect unsigned transaction - already signed");
        return response;
      }
      if (! Arrays.equals(Crypto.getPublicKey(secretPhrase), transaction.getSenderPublicKey())) {
        response.put(ERROR_CODE_RESPONSE, 4);
        response.put(ERROR_DESCRIPTION_RESPONSE, "Secret phrase doesn't match transaction sender public key");
        return response;
      }
      transaction.sign(secretPhrase);
      response.put(TRANSACTION_RESPONSE, transaction.getStringId());
      response.put(FULL_HASH_RESPONSE, transaction.getFullHash());
      response.put(TRANSACTION_BYTES_RESPONSE, Convert.toHexString(transaction.getBytes()));
      response.put(SIGNATURE_HASH_RESPONSE, Convert.toHexString(Crypto.sha256().digest(transaction.getSignature())));
      response.put(VERIFY_RESPONSE, transaction.verifySignature() && transactionService.verifyPublicKey(transaction));
    } catch (BurstException.ValidationException|RuntimeException e) {
      logger.debug(e.getMessage(), e);
      response.put(ERROR_CODE_RESPONSE, 4);
      response.put(ERROR_DESCRIPTION_RESPONSE, "Incorrect unsigned transaction: " + e.toString());
      response.put(ERROR_RESPONSE, e.getMessage());
      return response;
    }
    return response;
  }

}
