package brs.http;

import brs.*;
import brs.crypto.Crypto;
import brs.crypto.EncryptedData;
import brs.http.common.Parameters;
import brs.services.AccountService;
import brs.services.ParameterService;
import brs.util.Convert;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

import javax.servlet.http.HttpServletRequest;
import java.util.Arrays;

import static brs.http.JSONResponses.*;
import static brs.http.common.Parameters.BROADCAST_PARAMETER;
import static brs.http.common.Parameters.COMMENT_PARAMETER;
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
import static brs.http.common.Parameters.REFERENCED_TRANSACTION_PARAMETER;
import static brs.http.common.Parameters.SECRET_PHRASE_PARAMETER;
import static brs.http.common.ResultFields.BROADCASTED_RESPONSE;
import static brs.http.common.ResultFields.ERROR_RESPONSE;
import static brs.http.common.ResultFields.FULL_HASH_RESPONSE;
import static brs.http.common.ResultFields.SIGNATURE_HASH_RESPONSE;
import static brs.http.common.ResultFields.TRANSACTION_BYTES_RESPONSE;
import static brs.http.common.ResultFields.TRANSACTION_JSON_RESPONSE;
import static brs.http.common.ResultFields.TRANSACTION_RESPONSE;
import static brs.http.common.ResultFields.UNSIGNED_TRANSACTION_BYTES_RESPONSE;

abstract class CreateTransaction extends APIServlet.APIRequestHandler {

  private static final String[] commonParameters = new String[] {
    SECRET_PHRASE_PARAMETER, PUBLIC_KEY_PARAMETER, FEE_QT_PARAMETER,
    DEADLINE_PARAMETER, REFERENCED_TRANSACTION_FULL_HASH_PARAMETER, BROADCAST_PARAMETER,
    MESSAGE_PARAMETER, MESSAGE_IS_TEXT_PARAMETER,
    MESSAGE_TO_ENCRYPT_PARAMETER, MESSAGE_TO_ENCRYPT_IS_TEXT_PARAMETER, ENCRYPTED_MESSAGE_DATA_PARAMETER, ENCRYPTED_MESSAGE_NONCE_PARAMETER,
    MESSAGE_TO_ENCRYPT_TO_SELF_PARAMETER, MESSAGE_TO_ENCRYPT_TO_SELF_IS_TEXT_PARAMETER, ENCRYPT_TO_SELF_MESSAGE_DATA, ENCRYPT_TO_SELF_MESSAGE_NONCE,
    RECIPIENT_PUBLIC_KEY_PARAMETER};

  private final ParameterService parameterService;
  private final TransactionProcessor transactionProcessor;
  private final Blockchain blockchain;
  private final AccountService accountService;

  private static String[] addCommonParameters(String[] parameters) {
    String[] result = Arrays.copyOf(parameters, parameters.length + commonParameters.length);
    System.arraycopy(commonParameters, 0, result, parameters.length, commonParameters.length);
    return result;
  }

  CreateTransaction(APITag[] apiTags, ParameterService parameterService, TransactionProcessor transactionProcessor, Blockchain blockchain, AccountService accountService,
      String... parameters) {
    super(apiTags, addCommonParameters(parameters));
    this.parameterService = parameterService;
    this.transactionProcessor = transactionProcessor;
    this.blockchain = blockchain;
    this.accountService = accountService;
  }

  final JSONStreamAware createTransaction(HttpServletRequest req, Account senderAccount, Attachment attachment)
    throws BurstException {
    return createTransaction(req, senderAccount, null, 0, attachment);
  }

  final JSONStreamAware createTransaction(HttpServletRequest req, Account senderAccount, Long recipientId, long amountNQT)
    throws BurstException {
    return createTransaction(req, senderAccount, recipientId, amountNQT, Attachment.ORDINARY_PAYMENT);
  }

  final JSONStreamAware createTransaction(HttpServletRequest req, Account senderAccount, Long recipientId,
                                          long amountNQT, Attachment attachment)
    throws BurstException {
    int blockchainHeight = blockchain.getHeight();
    String deadlineValue = req.getParameter(DEADLINE_PARAMETER);
    String referencedTransactionFullHash = Convert.emptyToNull(req.getParameter(REFERENCED_TRANSACTION_FULL_HASH_PARAMETER));
    String referencedTransactionId = Convert.emptyToNull(req.getParameter(REFERENCED_TRANSACTION_PARAMETER));
    String secretPhrase = Convert.emptyToNull(req.getParameter(SECRET_PHRASE_PARAMETER));
    String publicKeyValue = Convert.emptyToNull(req.getParameter(PUBLIC_KEY_PARAMETER));
    boolean broadcast = !Parameters.isFalse(req.getParameter(BROADCAST_PARAMETER));

    Appendix.EncryptedMessage encryptedMessage = null;

    if (attachment.getTransactionType().hasRecipient()) {
      EncryptedData encryptedData = parameterService.getEncryptedMessage(req, accountService.getAccount(recipientId));
      if (encryptedData != null) {
        encryptedMessage = new Appendix.EncryptedMessage(encryptedData, !Parameters.isFalse(req.getParameter(MESSAGE_TO_ENCRYPT_IS_TEXT_PARAMETER)), blockchainHeight);
      }
    }

    Appendix.EncryptToSelfMessage encryptToSelfMessage = null;
    EncryptedData encryptedToSelfData = parameterService.getEncryptToSelfMessage(req);
    if (encryptedToSelfData != null) {
      encryptToSelfMessage = new Appendix.EncryptToSelfMessage(encryptedToSelfData, !Parameters.isFalse(req.getParameter(MESSAGE_TO_ENCRYPT_TO_SELF_IS_TEXT_PARAMETER)), blockchainHeight);
    }
    Appendix.Message message = null;
    String messageValue = Convert.emptyToNull(req.getParameter(MESSAGE_PARAMETER));
    if (messageValue != null) {
      boolean messageIsText = blockchainHeight >= Constants.DIGITAL_GOODS_STORE_BLOCK
          && !Parameters.isFalse(req.getParameter(MESSAGE_IS_TEXT_PARAMETER));
      try {
        message = messageIsText ? new Appendix.Message(messageValue, blockchainHeight) : new Appendix.Message(Convert.parseHexString(messageValue), blockchainHeight);
      } catch (RuntimeException e) {
        throw new ParameterException(INCORRECT_ARBITRARY_MESSAGE);
      }
    } else if (attachment instanceof Attachment.ColoredCoinsAssetTransfer && blockchainHeight >= Constants.DIGITAL_GOODS_STORE_BLOCK) {
      String commentValue = Convert.emptyToNull(req.getParameter(COMMENT_PARAMETER));
      if (commentValue != null) {
        message = new Appendix.Message(commentValue, blockchainHeight);
      }
    } else if (attachment == Attachment.ARBITRARY_MESSAGE && blockchainHeight < Constants.DIGITAL_GOODS_STORE_BLOCK) {
      message = new Appendix.Message(new byte[0], blockchainHeight);
    }
    Appendix.PublicKeyAnnouncement publicKeyAnnouncement = null;
    String recipientPublicKey = Convert.emptyToNull(req.getParameter(RECIPIENT_PUBLIC_KEY_PARAMETER));
    if (recipientPublicKey != null && blockchainHeight >= Constants.DIGITAL_GOODS_STORE_BLOCK) {
      publicKeyAnnouncement = new Appendix.PublicKeyAnnouncement(Convert.parseHexString(recipientPublicKey), blockchainHeight);
    }

    if (secretPhrase == null && publicKeyValue == null) {
      return MISSING_SECRET_PHRASE;
    } else if (deadlineValue == null) {
      return MISSING_DEADLINE;
    }

    short deadline;
    try {
      deadline = Short.parseShort(deadlineValue);
      if (deadline < 1 || deadline > 1440) {
        return INCORRECT_DEADLINE;
      }
    } catch (NumberFormatException e) {
      return INCORRECT_DEADLINE;
    }

    long feeNQT = ParameterParser.getFeeNQT(req);
    if (feeNQT < minimumFeeNQT()) {
      return INCORRECT_FEE;
    }

    try {
      if (Convert.safeAdd(amountNQT, feeNQT) > senderAccount.getUnconfirmedBalanceNQT()) {
        return NOT_ENOUGH_FUNDS;
      }
    } catch (ArithmeticException e) {
      return NOT_ENOUGH_FUNDS;
    }

    if (referencedTransactionId != null) {
      return INCORRECT_REFERENCED_TRANSACTION;
    }

    JSONObject response = new JSONObject();

    // shouldn't try to get publicKey from senderAccount as it may have not been set yet
    byte[] publicKey = secretPhrase != null ? Crypto.getPublicKey(secretPhrase) : Convert.parseHexString(publicKeyValue);

    try {
      Transaction.Builder builder = transactionProcessor.newTransactionBuilder(publicKey, amountNQT, feeNQT, deadline, attachment).referencedTransactionFullHash(referencedTransactionFullHash);
      if (attachment.getTransactionType().hasRecipient()) {
        builder.recipientId(recipientId);
      }
      if (encryptedMessage != null) {
        builder.encryptedMessage(encryptedMessage);
      }
      if (message != null) {
        builder.message(message);
      }
      if (publicKeyAnnouncement != null) {
        builder.publicKeyAnnouncement(publicKeyAnnouncement);
      }
      if (encryptToSelfMessage != null) {
        builder.encryptToSelfMessage(encryptToSelfMessage);
      }
      Transaction transaction = builder.build();
      transaction.validate();

      if (secretPhrase != null) {
        transaction.sign(secretPhrase);
        transaction.validate(); // 2nd validate may be needed if validation requires id to be known
        response.put(TRANSACTION_RESPONSE, transaction.getStringId());
        response.put(FULL_HASH_RESPONSE, transaction.getFullHash());
        response.put(TRANSACTION_BYTES_RESPONSE, Convert.toHexString(transaction.getBytes()));
        response.put(SIGNATURE_HASH_RESPONSE, Convert.toHexString(Crypto.sha256().digest(transaction.getSignature())));
        if (broadcast) {
          transactionProcessor.broadcast(transaction);
          response.put(BROADCASTED_RESPONSE, true);
        } else {
          response.put(BROADCASTED_RESPONSE, false);
        }
      } else {
        response.put(BROADCASTED_RESPONSE, false);
      }
      response.put(UNSIGNED_TRANSACTION_BYTES_RESPONSE, Convert.toHexString(transaction.getUnsignedBytes()));
      response.put(TRANSACTION_JSON_RESPONSE, JSONData.unconfirmedTransaction(transaction));

    } catch (BurstException.NotYetEnabledException e) {
      return FEATURE_NOT_AVAILABLE;
    } catch (BurstException.ValidationException e) {
      response.put(ERROR_RESPONSE, e.getMessage());
    }
    return response;

  }

  @Override
  final boolean requirePost() {
    return true;
  }

  long minimumFeeNQT() {
    return Constants.ONE_NXT;
  }

}
