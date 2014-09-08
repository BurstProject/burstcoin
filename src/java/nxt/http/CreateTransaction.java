package nxt.http;

import nxt.Account;
import nxt.Appendix;
import nxt.Attachment;
import nxt.Constants;
import nxt.Nxt;
import nxt.NxtException;
import nxt.Transaction;
import nxt.crypto.Crypto;
import nxt.crypto.EncryptedData;
import nxt.util.Convert;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

import javax.servlet.http.HttpServletRequest;
import java.util.Arrays;

import static nxt.http.JSONResponses.FEATURE_NOT_AVAILABLE;
import static nxt.http.JSONResponses.INCORRECT_ARBITRARY_MESSAGE;
import static nxt.http.JSONResponses.INCORRECT_DEADLINE;
import static nxt.http.JSONResponses.INCORRECT_FEE;
import static nxt.http.JSONResponses.INCORRECT_REFERENCED_TRANSACTION;
import static nxt.http.JSONResponses.MISSING_DEADLINE;
import static nxt.http.JSONResponses.MISSING_SECRET_PHRASE;
import static nxt.http.JSONResponses.NOT_ENOUGH_FUNDS;

abstract class CreateTransaction extends APIServlet.APIRequestHandler {

    private static final String[] commonParameters = new String[] {"secretPhrase", "publicKey", "feeNQT",
            "deadline", "referencedTransactionFullHash", "broadcast",
            "message", "messageIsText",
            "messageToEncrypt", "messageToEncryptIsText", "encryptedMessageData", "encryptedMessageNonce",
            "messageToEncryptToSelf", "messageToEncryptToSelfIsText", "encryptToSelfMessageData", "encryptToSelfMessageNonce",
            "recipientPublicKey"};

    private static String[] addCommonParameters(String[] parameters) {
        String[] result = Arrays.copyOf(parameters, parameters.length + commonParameters.length);
        System.arraycopy(commonParameters, 0, result, parameters.length, commonParameters.length);
        return result;
    }

    CreateTransaction(APITag[] apiTags, String... parameters) {
        super(apiTags, addCommonParameters(parameters));
    }

    final JSONStreamAware createTransaction(HttpServletRequest req, Account senderAccount, Attachment attachment)
        throws NxtException {
        return createTransaction(req, senderAccount, null, 0, attachment);
    }

    final JSONStreamAware createTransaction(HttpServletRequest req, Account senderAccount, Long recipientId, long amountNQT)
            throws NxtException {
        return createTransaction(req, senderAccount, recipientId, amountNQT, Attachment.ORDINARY_PAYMENT);
    }

    final JSONStreamAware createTransaction(HttpServletRequest req, Account senderAccount, Long recipientId,
                                            long amountNQT, Attachment attachment)
            throws NxtException {
        String deadlineValue = req.getParameter("deadline");
        String referencedTransactionFullHash = Convert.emptyToNull(req.getParameter("referencedTransactionFullHash"));
        String referencedTransactionId = Convert.emptyToNull(req.getParameter("referencedTransaction"));
        String secretPhrase = Convert.emptyToNull(req.getParameter("secretPhrase"));
        String publicKeyValue = Convert.emptyToNull(req.getParameter("publicKey"));
        boolean broadcast = !"false".equalsIgnoreCase(req.getParameter("broadcast"));
        Appendix.EncryptedMessage encryptedMessage = null;
        if (attachment.getTransactionType().hasRecipient()) {
            EncryptedData encryptedData = ParameterParser.getEncryptedMessage(req, Account.getAccount(recipientId));
            if (encryptedData != null) {
                encryptedMessage = new Appendix.EncryptedMessage(encryptedData, !"false".equalsIgnoreCase(req.getParameter("messageToEncryptIsText")));
            }
        }
        Appendix.EncryptToSelfMessage encryptToSelfMessage = null;
        EncryptedData encryptedToSelfData = ParameterParser.getEncryptToSelfMessage(req);
        if (encryptedToSelfData != null) {
            encryptToSelfMessage = new Appendix.EncryptToSelfMessage(encryptedToSelfData, !"false".equalsIgnoreCase(req.getParameter("messageToEncryptToSelfIsText")));
        }
        Appendix.Message message = null;
        String messageValue = Convert.emptyToNull(req.getParameter("message"));
        if (messageValue != null) {
            boolean messageIsText = !"false".equalsIgnoreCase(req.getParameter("messageIsText"));
            try {
                message = messageIsText ? new Appendix.Message(messageValue) : new Appendix.Message(Convert.parseHexString(messageValue));
            } catch (RuntimeException e) {
                throw new ParameterException(INCORRECT_ARBITRARY_MESSAGE);
            }
        }
        Appendix.PublicKeyAnnouncement publicKeyAnnouncement = null;
        String recipientPublicKey = Convert.emptyToNull(req.getParameter("recipientPublicKey"));
        if (recipientPublicKey != null) {
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
            Transaction.Builder builder = Nxt.getTransactionProcessor().newTransactionBuilder(publicKey, amountNQT, feeNQT,
                    deadline, attachment).referencedTransactionFullHash(referencedTransactionFullHash);
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
                response.put("transaction", transaction.getStringId());
                response.put("fullHash", transaction.getFullHash());
                response.put("transactionBytes", Convert.toHexString(transaction.getBytes()));
                response.put("signatureHash", Convert.toHexString(Crypto.sha256().digest(transaction.getSignature())));
                if (broadcast) {
                    Nxt.getTransactionProcessor().broadcast(transaction);
                    response.put("broadcasted", true);
                } else {
                    response.put("broadcasted", false);
                }
            } else {
                response.put("broadcasted", false);
            }
            response.put("unsignedTransactionBytes", Convert.toHexString(transaction.getUnsignedBytes()));
            response.put("transactionJSON", JSONData.unconfirmedTransaction(transaction));

        } catch (NxtException.NotYetEnabledException e) {
            return FEATURE_NOT_AVAILABLE;
        } catch (NxtException.ValidationException e) {
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
