package brs.http;

import static brs.http.JSONResponses.DECRYPTION_FAILED;
import static brs.http.JSONResponses.INCORRECT_ACCOUNT;
import static brs.http.common.Parameters.ACCOUNT_PARAMETER;
import static brs.http.common.Parameters.DATA_PARAMETER;
import static brs.http.common.Parameters.DECRYPTED_MESSAGE_IS_TEXT_PARAMETER;
import static brs.http.common.Parameters.NONCE_PARAMETER;
import static brs.http.common.Parameters.SECRET_PHRASE_PARAMETER;
import static brs.http.common.ResultFields.DECRYPTED_MESSAGE_RESPONSE;

import brs.Account;
import brs.BurstException;
import brs.crypto.EncryptedData;
import brs.http.common.Parameters;
import brs.services.ParameterService;
import brs.util.Convert;
import javax.servlet.http.HttpServletRequest;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class DecryptFrom extends APIServlet.APIRequestHandler {

  private static final Logger logger = LoggerFactory.getLogger(DecryptFrom.class);

  private final ParameterService parameterService;

  DecryptFrom(ParameterService parameterService) {
    super(new APITag[]{APITag.MESSAGES}, ACCOUNT_PARAMETER, DATA_PARAMETER, NONCE_PARAMETER, DECRYPTED_MESSAGE_IS_TEXT_PARAMETER, SECRET_PHRASE_PARAMETER);
    this.parameterService = parameterService;
  }

  @Override
  JSONStreamAware processRequest(HttpServletRequest req) throws BurstException {
    Account account = parameterService.getAccount(req);
    if (account.getPublicKey() == null) {
      return INCORRECT_ACCOUNT;
    }
    String secretPhrase = ParameterParser.getSecretPhrase(req);
    byte[] data = Convert.parseHexString(Convert.nullToEmpty(req.getParameter(DATA_PARAMETER)));
    byte[] nonce = Convert.parseHexString(Convert.nullToEmpty(req.getParameter(NONCE_PARAMETER)));
    EncryptedData encryptedData = new EncryptedData(data, nonce);
    boolean isText = Parameters.isFalse(req.getParameter(DECRYPTED_MESSAGE_IS_TEXT_PARAMETER));
    try {
      byte[] decrypted = account.decryptFrom(encryptedData, secretPhrase);
      JSONObject response = new JSONObject();
      response.put(DECRYPTED_MESSAGE_RESPONSE, isText ? Convert.toString(decrypted) : Convert.toHexString(decrypted));
      return response;
    } catch (RuntimeException e) {
      logger.debug(e.toString());
      return DECRYPTION_FAILED;
    }
  }

}
