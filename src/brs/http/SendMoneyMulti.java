package brs.http;

import static brs.http.common.Parameters.BROADCAST_PARAMETER;
import static brs.http.common.Parameters.DEADLINE_PARAMETER;
import static brs.http.common.Parameters.ENCRYPTED_MESSAGE_DATA_PARAMETER;
import static brs.http.common.Parameters.ENCRYPTED_MESSAGE_NONCE_PARAMETER;
import static brs.http.common.Parameters.ENCRYPT_TO_SELF_MESSAGE_DATA;
import static brs.http.common.Parameters.ENCRYPT_TO_SELF_MESSAGE_NONCE;
import static brs.http.common.Parameters.FEE_QT_PARAMETER;
import static brs.http.common.Parameters.MESSAGE_IS_TEXT_PARAMETER;
import static brs.http.common.Parameters.MESSAGE_PARAMETER;
import static brs.http.common.Parameters.MESSAGE_TO_ENCRYPT_IS_TEXT_PARAMETER;
import static brs.http.common.Parameters.MESSAGE_TO_ENCRYPT_PARAMETER;
import static brs.http.common.Parameters.MESSAGE_TO_ENCRYPT_TO_SELF_IS_TEXT_PARAMETER;
import static brs.http.common.Parameters.MESSAGE_TO_ENCRYPT_TO_SELF_PARAMETER;
import static brs.http.common.Parameters.PUBLIC_KEY_PARAMETER;
import static brs.http.common.Parameters.RECIPIENTS_PARAMETER;
import static brs.http.common.Parameters.RECIPIENT_PUBLIC_KEY_PARAMETER;
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

public final class SendMoneyMulti extends CreateTransaction {

  private static final String[] commonParameters = new String[] {
      SECRET_PHRASE_PARAMETER, PUBLIC_KEY_PARAMETER, FEE_QT_PARAMETER,
      DEADLINE_PARAMETER, REFERENCED_TRANSACTION_FULL_HASH_PARAMETER, BROADCAST_PARAMETER,
      RECIPIENTS_PARAMETER};

  private final ParameterService parameterService;
  private final Blockchain blockchain;

  SendMoneyMulti(ParameterService parameterService, Blockchain blockchain, APITransactionManager apiTransactionManager) {
    super(new APITag[] {APITag.TRANSACTIONS, APITag.CREATE_TRANSACTION}, apiTransactionManager, true, commonParameters);

    this.parameterService = parameterService;
    this.blockchain = blockchain;
  }
	
  @Override
  JSONStreamAware processRequest(HttpServletRequest req) throws BurstException {
    Account sender = parameterService.getSenderAccount(req);
    String recipientString = Convert.emptyToNull(req.getParameter(RECIPIENTS_PARAMETER));

    if(recipientString == null) {
      JSONObject response = new JSONObject();
      response.put(ERROR_CODE_RESPONSE, 3);
      response.put(ERROR_DESCRIPTION_RESPONSE, "Recipients not specified");
      return response;
    }
		
    String transactionArray[] = recipientString.split(";", Constants.MAX_MULTI_OUT_RECIPIENTS);

    if(transactionArray.length > Constants.MAX_MULTI_OUT_RECIPIENTS || transactionArray.length < 2) {
      JSONObject response = new JSONObject();
      response.put(ERROR_CODE_RESPONSE, 4);
      response.put(ERROR_DESCRIPTION_RESPONSE, "Invalid number of recipients");
      return response;
    }
		
    Collection<Entry<String, Long>> recipients = new ArrayList<>();

    long totalAmountNQT = 0;
    try {
      for(String transactionString : transactionArray) {
        String recipientArray[] = transactionString.split(":", 2);
        Long recipientId = Convert.parseUnsignedLong(recipientArray[0]);
        Long amountNQT   = Convert.parseUnsignedLong(recipientArray[1]);
        recipients.add( new SimpleEntry<String,Long>("" + recipientId, amountNQT) );
        totalAmountNQT += amountNQT;
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

    Attachment.PaymentMultiOutCreation attachment = new Attachment.PaymentMultiOutCreation(recipients, blockchain.getHeight());

    return createTransaction(req, sender, null, attachment.getAmountNQT(), attachment);
  }
}
