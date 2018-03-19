package brs.http;

import brs.*;
import org.json.simple.JSONStreamAware;

import javax.servlet.http.HttpServletRequest;
import java.util.Arrays;

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
import static brs.http.common.Parameters.RECIPIENT_PUBLIC_KEY_PARAMETER;
import static brs.http.common.Parameters.REFERENCED_TRANSACTION_FULL_HASH_PARAMETER;
import static brs.http.common.Parameters.SECRET_PHRASE_PARAMETER;

abstract class CreateTransaction extends APIServlet.APIRequestHandler {

  private static final String[] commonParameters = new String[] {
    SECRET_PHRASE_PARAMETER, PUBLIC_KEY_PARAMETER, FEE_QT_PARAMETER,
    DEADLINE_PARAMETER, REFERENCED_TRANSACTION_FULL_HASH_PARAMETER, BROADCAST_PARAMETER,
    MESSAGE_PARAMETER, MESSAGE_IS_TEXT_PARAMETER,
    MESSAGE_TO_ENCRYPT_PARAMETER, MESSAGE_TO_ENCRYPT_IS_TEXT_PARAMETER, ENCRYPTED_MESSAGE_DATA_PARAMETER, ENCRYPTED_MESSAGE_NONCE_PARAMETER,
    MESSAGE_TO_ENCRYPT_TO_SELF_PARAMETER, MESSAGE_TO_ENCRYPT_TO_SELF_IS_TEXT_PARAMETER, ENCRYPT_TO_SELF_MESSAGE_DATA, ENCRYPT_TO_SELF_MESSAGE_NONCE,
    RECIPIENT_PUBLIC_KEY_PARAMETER};

  private final APITransactionManager apiTransactionManager;

  private static String[] addCommonParameters(String[] parameters) {
    String[] result = Arrays.copyOf(parameters, parameters.length + commonParameters.length);
    System.arraycopy(commonParameters, 0, result, parameters.length, commonParameters.length);
    return result;
  }

  CreateTransaction(APITag[] apiTags, APITransactionManager apiTransactionManager, String... parameters) {
    super(apiTags, addCommonParameters(parameters));
    this.apiTransactionManager = apiTransactionManager;
  }

  final JSONStreamAware createTransaction(HttpServletRequest req, Account senderAccount, Attachment attachment)
    throws BurstException {
    return createTransaction(req, senderAccount, null, 0, attachment);
  }

  final JSONStreamAware createTransaction(HttpServletRequest req, Account senderAccount, Long recipientId, long amountNQT)
    throws BurstException {
    return createTransaction(req, senderAccount, recipientId, amountNQT, Attachment.ORDINARY_PAYMENT);
  }

  final JSONStreamAware createTransaction(HttpServletRequest req, Account senderAccount, Long recipientId, long amountNQT, Attachment attachment) throws BurstException {
    return apiTransactionManager.createTransaction(req, senderAccount, recipientId, amountNQT, attachment, minimumFeeNQT());
  }

  @Override
  final boolean requirePost() {
    return true;
  }

  long minimumFeeNQT() {
    return Constants.ONE_BURST;
  }

}
