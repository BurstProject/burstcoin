package brs.http;

import brs.*;
import brs.crypto.Crypto;
import brs.crypto.EncryptedData;
import brs.http.common.Parameters;
import brs.services.ParameterService;
import brs.util.Convert;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

import javax.servlet.http.HttpServletRequest;
import java.util.Arrays;

import static brs.http.JSONResponses.*;
import static brs.http.common.Parameters.BROADCAST_PARAMETER;
import static brs.http.common.Parameters.DEADLINE_PARAMETER;
import static brs.http.common.Parameters.MESSAGE_TO_ENCRYPT_IS_TEXT_PARAMETER;
import static brs.http.common.Parameters.PUBLIC_KEY_PARAMETER;
import static brs.http.common.Parameters.REFERENCED_TRANSACTION_FULL_HASH_PARAMETER;
import static brs.http.common.Parameters.REFERENCED_TRANSACTION_PARAMETER;
import static brs.http.common.Parameters.SECRET_PHRASE_PARAMETER;

abstract class CreateTransaction extends APIServlet.APIRequestHandler {

  private static final String[] commonParameters = new String[] {
    "secretPhrase", "publicKey", "feeNQT",
    "deadline", "referencedTransactionFullHash", "broadcast",
    "message", "messageIsText",
    "messageToEncrypt", "messageToEncryptIsText", "encryptedMessageData", "encryptedMessageNonce",
    "messageToEncryptToSelf", "messageToEncryptToSelfIsText", "encryptToSelfMessageData", "encryptToSelfMessageNonce",
    "recipientPublicKey"};

  private final ParameterService parameterService;
  private final TransactionProcessor transactionProcessor;
  private final Blockchain blockchain;

  private static String[] addCommonParameters(String[] parameters) {
    String[] result = Arrays.copyOf(parameters, parameters.length + commonParameters.length);
    System.arraycopy(commonParameters, 0, result, parameters.length, commonParameters.length);
    return result;
  }

  CreateTransaction(APITag[] apiTags, ParameterService parameterService, TransactionProcessor transactionProcessor, Blockchain blockchain, String... parameters) {
    super(apiTags, addCommonParameters(parameters));
    this.parameterService = parameterService;
    this.transactionProcessor = transactionProcessor;
    this.blockchain = blockchain;
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
      EncryptedData encryptedData = parameterService.getEncryptedMessage(req, Account.getAccount(recipientId));
      if (encryptedData != null) {
        encryptedMessage = new Appendix.EncryptedMessage(encryptedData, !Parameters.isFalse(req.getParameter(MESSAGE_TO_ENCRYPT_IS_TEXT_PARAMETER)));
      }
    }

    Appendix.EncryptToSelfMessage encryptToSelfMessage = null;
    EncryptedData encryptedToSelfData = parameterService.getEncryptToSelfMessage(req);
    if (encryptedToSelfData != null) {
      encryptToSelfMessage = new Appendix.EncryptToSelfMessage(encryptedToSelfData, !"false".equalsIgnoreCase(req.getParameter("messageToEncryptToSelfIsText")));
    }
    Appendix.Message message = null;
    String messageValue = Convert.emptyToNull(req.getParameter("message"));
    if (messageValue != null) {
      boolean messageIsText = blockchainHeight >= Constants.DIGITAL_GOODS_STORE_BLOCK
          && !"false".equalsIgnoreCase(req.getParameter("messageIsText"));
      try {
        message = messageIsText ? new Appendix.Message(messageValue) : new Appendix.Message(Convert.parseHexString(messageValue));
      } catch (RuntimeException e) {
        throw new ParameterException(INCORRECT_ARBITRARY_MESSAGE);
      }
    } else if (attachment instanceof Attachment.ColoredCoinsAssetTransfer && blockchainHeight >= Constants.DIGITAL_GOODS_STORE_BLOCK) {
      String commentValue = Convert.emptyToNull(req.getParameter("comment"));
      if (commentValue != null) {
        message = new Appendix.Message(commentValue);
      }
    } else if (attachment == Attachment.ARBITRARY_MESSAGE && blockchainHeight < Constants.DIGITAL_GOODS_STORE_BLOCK) {
      message = new Appendix.Message(new byte[0]);
    }
    Appendix.PublicKeyAnnouncement publicKeyAnnouncement = null;
    String recipientPublicKey = Convert.emptyToNull(req.getParameter("recipientPublicKey"));
    if (recipientPublicKey != null && blockchainHeight >= Constants.DIGITAL_GOODS_STORE_BLOCK) {
      publicKeyAnnouncement = new Appendix.PublicKeyAnnouncement(Convert.parseHexString(recipientPublicKey));
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
        response.put("transaction", transaction.getStringId());
        response.put("fullHash", transaction.getFullHash());
        response.put("transactionBytes", Convert.toHexString(transaction.getBytes()));
        response.put("signatureHash", Convert.toHexString(Crypto.sha256().digest(transaction.getSignature())));
        if (broadcast) {
          transactionProcessor.broadcast(transaction);
          response.put("broadcasted", true);
        } else {
          response.put("broadcasted", false);
        }
      } else {
        response.put("broadcasted", false);
      }
      response.put("unsignedTransactionBytes", Convert.toHexString(transaction.getUnsignedBytes()));
      response.put("transactionJSON", JSONData.unconfirmedTransaction(transaction));

    } catch (BurstException.NotYetEnabledException e) {
      return FEATURE_NOT_AVAILABLE;
    } catch (BurstException.ValidationException e) {
      response.put("error", e.getMessage());
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
