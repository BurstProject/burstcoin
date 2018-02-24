package brs.http;

import static brs.http.JSONResponses.FEATURE_NOT_AVAILABLE;
import static brs.http.JSONResponses.INCORRECT_ARBITRARY_MESSAGE;
import static brs.http.JSONResponses.INCORRECT_DEADLINE;
import static brs.http.JSONResponses.INCORRECT_FEE;
import static brs.http.JSONResponses.INCORRECT_REFERENCED_TRANSACTION;
import static brs.http.JSONResponses.MISSING_DEADLINE;
import static brs.http.JSONResponses.MISSING_SECRET_PHRASE;
import static brs.http.JSONResponses.NOT_ENOUGH_FUNDS;
import static brs.http.common.Parameters.BROADCAST_PARAMETER;
import static brs.http.common.Parameters.COMMENT_PARAMETER;
import static brs.http.common.Parameters.DEADLINE_PARAMETER;
import static brs.http.common.Parameters.MESSAGE_IS_TEXT_PARAMETER;
import static brs.http.common.Parameters.MESSAGE_PARAMETER;
import static brs.http.common.Parameters.MESSAGE_TO_ENCRYPT_IS_TEXT_PARAMETER;
import static brs.http.common.Parameters.MESSAGE_TO_ENCRYPT_TO_SELF_IS_TEXT_PARAMETER;
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

import brs.Account;
import brs.Appendix.EncryptToSelfMessage;
import brs.Appendix.EncryptedMessage;
import brs.Appendix.Message;
import brs.Appendix.PublicKeyAnnouncement;
import brs.Attachment;
import brs.Blockchain;
import brs.BurstException;
import brs.Constants;
import brs.Transaction;
import brs.Transaction.Builder;
import brs.TransactionProcessor;
import brs.crypto.Crypto;
import brs.crypto.EncryptedData;
import brs.http.common.Parameters;
import brs.services.AccountService;
import brs.services.ParameterService;
import brs.services.TransactionService;
import brs.util.Convert;
import javax.servlet.http.HttpServletRequest;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

public class APITransactionManagerImpl implements APITransactionManager {

  private final ParameterService parameterService;
  private final TransactionProcessor transactionProcessor;
  private final Blockchain blockchain;
  private final AccountService accountService;
  private final TransactionService transactionService;


  public APITransactionManagerImpl(ParameterService parameterService, TransactionProcessor transactionProcessor, Blockchain blockchain, AccountService accountService,
      TransactionService transactionService) {
    this.parameterService = parameterService;
    this.transactionProcessor = transactionProcessor;
    this.blockchain = blockchain;
    this.accountService = accountService;
    this.transactionService = transactionService;
  }

  @Override
  public JSONStreamAware createTransaction(HttpServletRequest req, Account senderAccount, Long recipientId, long amountNQT, Attachment attachment, long minimumFeeNQT) throws BurstException {
    int blockchainHeight = blockchain.getHeight();
    String deadlineValue = req.getParameter(DEADLINE_PARAMETER);
    String referencedTransactionFullHash = Convert.emptyToNull(req.getParameter(REFERENCED_TRANSACTION_FULL_HASH_PARAMETER));
    String referencedTransactionId = Convert.emptyToNull(req.getParameter(REFERENCED_TRANSACTION_PARAMETER));
    String secretPhrase = Convert.emptyToNull(req.getParameter(SECRET_PHRASE_PARAMETER));
    String publicKeyValue = Convert.emptyToNull(req.getParameter(PUBLIC_KEY_PARAMETER));
    boolean broadcast = !Parameters.isFalse(req.getParameter(BROADCAST_PARAMETER));

    EncryptedMessage encryptedMessage = null;

    if (attachment.getTransactionType().hasRecipient()) {
      EncryptedData encryptedData = parameterService.getEncryptedMessage(req, accountService.getAccount(recipientId));
      if (encryptedData != null) {
        encryptedMessage = new EncryptedMessage(encryptedData, !Parameters.isFalse(req.getParameter(MESSAGE_TO_ENCRYPT_IS_TEXT_PARAMETER)), blockchainHeight);
      }
    }

    EncryptToSelfMessage encryptToSelfMessage = null;
    EncryptedData encryptedToSelfData = parameterService.getEncryptToSelfMessage(req);
    if (encryptedToSelfData != null) {
      encryptToSelfMessage = new EncryptToSelfMessage(encryptedToSelfData, !Parameters.isFalse(req.getParameter(MESSAGE_TO_ENCRYPT_TO_SELF_IS_TEXT_PARAMETER)), blockchainHeight);
    }
    Message message = null;
    String messageValue = Convert.emptyToNull(req.getParameter(MESSAGE_PARAMETER));
    if (messageValue != null) {
      boolean messageIsText = blockchainHeight >= Constants.DIGITAL_GOODS_STORE_BLOCK
          && !Parameters.isFalse(req.getParameter(MESSAGE_IS_TEXT_PARAMETER));
      try {
        message = messageIsText ? new Message(messageValue, blockchainHeight) : new Message(Convert.parseHexString(messageValue), blockchainHeight);
      } catch (RuntimeException e) {
        throw new ParameterException(INCORRECT_ARBITRARY_MESSAGE);
      }
    } else if (attachment instanceof Attachment.ColoredCoinsAssetTransfer && blockchainHeight >= Constants.DIGITAL_GOODS_STORE_BLOCK) {
      String commentValue = Convert.emptyToNull(req.getParameter(COMMENT_PARAMETER));
      if (commentValue != null) {
        message = new Message(commentValue, blockchainHeight);
      }
    } else if (attachment == Attachment.ARBITRARY_MESSAGE && blockchainHeight < Constants.DIGITAL_GOODS_STORE_BLOCK) {
      message = new Message(new byte[0], blockchainHeight);
    }
    PublicKeyAnnouncement publicKeyAnnouncement = null;
    String recipientPublicKey = Convert.emptyToNull(req.getParameter(RECIPIENT_PUBLIC_KEY_PARAMETER));
    if (recipientPublicKey != null && blockchainHeight >= Constants.DIGITAL_GOODS_STORE_BLOCK) {
      publicKeyAnnouncement = new PublicKeyAnnouncement(Convert.parseHexString(recipientPublicKey), blockchainHeight);
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
    if (feeNQT < minimumFeeNQT) {
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
      Builder builder = transactionProcessor.newTransactionBuilder(publicKey, amountNQT, feeNQT, deadline, attachment).referencedTransactionFullHash(referencedTransactionFullHash);
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
      transactionService.validate(transaction);

      if (secretPhrase != null) {
        transaction.sign(secretPhrase);
        transactionService.validate(transaction); // 2nd validate may be needed if validation requires id to be known
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
}
