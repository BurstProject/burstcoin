package brs.http;

import static brs.http.JSONResponses.INCORRECT_RECIPIENT;
import static brs.http.common.Parameters.MESSAGE_TO_ENCRYPT_IS_TEXT_PARAMETER;
import static brs.http.common.Parameters.MESSAGE_TO_ENCRYPT_PARAMETER;
import static brs.http.common.Parameters.RECIPIENT_PARAMETER;
import static brs.http.common.Parameters.SECRET_PHRASE_PARAMETER;

import brs.Account;
import brs.BurstException;
import brs.crypto.EncryptedData;
import brs.services.AccountService;
import brs.services.ParameterService;
import javax.servlet.http.HttpServletRequest;
import org.json.simple.JSONStreamAware;

public final class EncryptTo extends APIServlet.APIRequestHandler {

  private final ParameterService parameterService;
  private final AccountService accountService;

  EncryptTo(ParameterService parameterService, AccountService accountService) {
    super(new APITag[]{APITag.MESSAGES}, RECIPIENT_PARAMETER, MESSAGE_TO_ENCRYPT_PARAMETER, MESSAGE_TO_ENCRYPT_IS_TEXT_PARAMETER, SECRET_PHRASE_PARAMETER);
    this.parameterService = parameterService;
    this.accountService = accountService;
  }

  @Override
  JSONStreamAware processRequest(HttpServletRequest req) throws BurstException {

    long recipientId = ParameterParser.getRecipientId(req);
    Account recipientAccount = accountService.getAccount(recipientId);
    if (recipientAccount == null || recipientAccount.getPublicKey() == null) {
      return INCORRECT_RECIPIENT;
    }

    EncryptedData encryptedData = parameterService.getEncryptedMessage(req, recipientAccount);
    return JSONData.encryptedData(encryptedData);

  }

}
