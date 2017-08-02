package nxt.http;

import nxt.Account;
import nxt.Appendix;
import nxt.Nxt;
import nxt.Transaction;
import nxt.crypto.Crypto;
import nxt.util.Convert;
import nxt.util.LoggerConfigurator;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.http.HttpServletRequest;

import static nxt.http.JSONResponses.INCORRECT_TRANSACTION;
import static nxt.http.JSONResponses.MISSING_TRANSACTION;
import static nxt.http.JSONResponses.NO_MESSAGE;
import static nxt.http.JSONResponses.UNKNOWN_TRANSACTION;

public final class ReadMessage extends APIServlet.APIRequestHandler {

    private static final Logger logger = LoggerFactory.getLogger(ReadMessage.class);

    static final ReadMessage instance = new ReadMessage();

    private ReadMessage() {
        super(new APITag[] {APITag.MESSAGES}, "transaction", "secretPhrase");
    }

    @Override
    JSONStreamAware processRequest(HttpServletRequest req) throws ParameterException {

        String transactionIdString = Convert.emptyToNull(req.getParameter("transaction"));
        if (transactionIdString == null) {
            return MISSING_TRANSACTION;
        }

        Transaction transaction;
        try {
            transaction = Nxt.getBlockchain().getTransaction(Convert.parseUnsignedLong(transactionIdString));
            if (transaction == null) {
                return UNKNOWN_TRANSACTION;
            }
        } catch (RuntimeException e) {
            return INCORRECT_TRANSACTION;
        }

        JSONObject response = new JSONObject();
        Account senderAccount = Account.getAccount(transaction.getSenderId());
        Appendix.Message message = transaction.getMessage();
        Appendix.EncryptedMessage encryptedMessage = transaction.getEncryptedMessage();
        Appendix.EncryptToSelfMessage encryptToSelfMessage = transaction.getEncryptToSelfMessage();
        if (message == null && encryptedMessage == null && encryptToSelfMessage == null) {
            return NO_MESSAGE;
        }
        if (message != null) {
            response.put("message", message.isText() ? Convert.toString(message.getMessage()) : Convert.toHexString(message.getMessage()));
        }
        String secretPhrase = Convert.emptyToNull(req.getParameter("secretPhrase"));
        if (secretPhrase != null) {
            if (encryptedMessage != null) {
                long readerAccountId = Account.getId(Crypto.getPublicKey(secretPhrase));
                Account account = senderAccount.getId() == readerAccountId ? Account.getAccount(transaction.getRecipientId()) : senderAccount;
                if (account != null) {
                    try {
                        byte[] decrypted = account.decryptFrom(encryptedMessage.getEncryptedData(), secretPhrase);
                        response.put("decryptedMessage", encryptedMessage.isText() ? Convert.toString(decrypted) : Convert.toHexString(decrypted));
                    } catch (RuntimeException e) {
                        logger.debug("Decryption of message to recipient failed: " + e.toString());
                    }
                }
            }
            if (encryptToSelfMessage != null) {
                Account account = Account.getAccount(Crypto.getPublicKey(secretPhrase));
                if (account != null) {
                    try {
                        byte[] decrypted = account.decryptFrom(encryptToSelfMessage.getEncryptedData(), secretPhrase);
                        response.put("decryptedMessageToSelf", encryptToSelfMessage.isText() ? Convert.toString(decrypted) : Convert.toHexString(decrypted));
                    } catch (RuntimeException e) {
                        logger.debug("Decryption of message to self failed: " + e.toString());
                    }
                }
            }
        }
        return response;
    }

}
