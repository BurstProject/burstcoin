package brs.http;

import brs.Account;
import brs.Attachment;
import brs.Blockchain;
import brs.BurstException;
import brs.TransactionProcessor;
import brs.services.ParameterService;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

import javax.servlet.http.HttpServletRequest;

public final class SetRewardRecipient extends CreateTransaction {

  private final ParameterService parameterService;
	
  public SetRewardRecipient(ParameterService parameterService, TransactionProcessor transactionProcessor, Blockchain blockchain) {
    super(new APITag[] {APITag.ACCOUNTS, APITag.MINING, APITag.CREATE_TRANSACTION}, parameterService, transactionProcessor, blockchain, "recipient");
    this.parameterService = parameterService;
  }
	
  @Override
  JSONStreamAware processRequest(HttpServletRequest req) throws BurstException {
    final Account account = parameterService.getSenderAccount(req);
    Long recipient = ParameterParser.getRecipientId(req);
    Account recipientAccount = Account.getAccount(recipient);
    if (recipientAccount == null || recipientAccount.getPublicKey() == null) {
      JSONObject response = new JSONObject();
      response.put("errorCode", 8);
      response.put("errorDescription", "recipient account does not have public key");
      return response;
    }
    Attachment attachment = new Attachment.BurstMiningRewardRecipientAssignment();
    return createTransaction(req, account, recipient, 0, attachment);
  }

}
