package brs.http;

import static brs.http.common.Parameters.AMOUNT_NQT_PARAMETER;
import static brs.http.common.Parameters.BROADCAST_PARAMETER;
import static brs.http.common.Parameters.DEADLINE_PARAMETER;
import static brs.http.common.Parameters.FEE_QT_PARAMETER;
import static brs.http.common.Parameters.PUBLIC_KEY_PARAMETER;
import static brs.http.common.Parameters.RECIPIENTS_PARAMETER;
import static brs.http.common.Parameters.REFERENCED_TRANSACTION_FULL_HASH_PARAMETER;
import static brs.http.common.Parameters.SECRET_PHRASE_PARAMETER;
import static brs.http.common.ResultFields.ERROR_CODE_RESPONSE;
import static brs.http.common.ResultFields.ERROR_DESCRIPTION_RESPONSE;

import brs.Account;
import brs.Attachment;
import brs.Blockchain;
import brs.BurstException;
import brs.Constants;
import brs.services.ParameterService;
import brs.util.Convert;
import java.util.AbstractMap.SimpleEntry;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Map.Entry;
import javax.servlet.http.HttpServletRequest;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

public final class SendMoneyMultiSame extends CreateTransaction {

  private static final String[] commonParameters = new String[] {
      SECRET_PHRASE_PARAMETER, PUBLIC_KEY_PARAMETER, FEE_QT_PARAMETER,
      DEADLINE_PARAMETER, REFERENCED_TRANSACTION_FULL_HASH_PARAMETER, BROADCAST_PARAMETER,
      RECIPIENTS_PARAMETER, AMOUNT_NQT_PARAMETER};

  private final ParameterService parameterService;
  private final Blockchain blockchain;

  SendMoneyMultiSame(ParameterService parameterService, Blockchain blockchain, APITransactionManager apiTransactionManager) {
    super(new APITag[] {APITag.TRANSACTIONS, APITag.CREATE_TRANSACTION}, apiTransactionManager, true, commonParameters);

    this.parameterService = parameterService;
    this.blockchain = blockchain;
  }
	
  @Override
  JSONStreamAware processRequest(HttpServletRequest req) throws BurstException {
    long amountNQT = ParameterParser.getAmountNQT(req);
    Account sender = parameterService.getSenderAccount(req);
    String recipientString = Convert.emptyToNull(req.getParameter(RECIPIENTS_PARAMETER));


    if(recipientString == null) {
      JSONObject response = new JSONObject();
      response.put(ERROR_CODE_RESPONSE, 3);
      response.put(ERROR_DESCRIPTION_RESPONSE, "Recipients not specified");
      return response;
    }
		
    String recipientsArray[] = recipientString.split(";", Constants.MAX_MULTI_OUT_RECIPIENTS);

    if(recipientsArray.length > Constants.MAX_MULTI_SAME_OUT_RECIPIENTS || recipientsArray.length < 2) {
      JSONObject response = new JSONObject();
      response.put(ERROR_CODE_RESPONSE, 4);
      response.put(ERROR_DESCRIPTION_RESPONSE, "Invalid number of recipients");
      return response;
    }

    Collection<Long> recipients = new ArrayList<>();

    long totalAmountNQT = amountNQT * recipientsArray.length;
    try {
      for(String recipientId : recipientsArray) {
        recipients.add(Convert.parseUnsignedLong(recipientId));
      }
    }
    catch(Exception e) {
      JSONObject response = new JSONObject();
      response.put(ERROR_CODE_RESPONSE, 4);
      response.put(ERROR_DESCRIPTION_RESPONSE, "Invalid recipients parameter");
      return response;
    }
		
    if(sender.getBalanceNQT() < totalAmountNQT) {
      JSONObject response = new JSONObject();
      response.put(ERROR_CODE_RESPONSE, 6);
      response.put(ERROR_DESCRIPTION_RESPONSE, "Insufficient funds");
      return response;
    }

    Attachment.PaymentMultiSameOutCreation attachment = new Attachment.PaymentMultiSameOutCreation(recipients, blockchain.getHeight());

    return createTransaction(req, sender, null, totalAmountNQT, attachment);
  }
}
