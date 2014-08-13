package nxt.http;

import nxt.Nxt;
import nxt.NxtException;
import nxt.Transaction;
import nxt.crypto.Crypto;
import nxt.util.Convert;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

import javax.servlet.http.HttpServletRequest;

import static nxt.http.JSONResponses.INCORRECT_UNSIGNED_BYTES;
import static nxt.http.JSONResponses.MISSING_SECRET_PHRASE;
import static nxt.http.JSONResponses.MISSING_UNSIGNED_BYTES;

public final class SignTransaction extends APIServlet.APIRequestHandler {

    static final SignTransaction instance = new SignTransaction();

    private SignTransaction() {
        super("unsignedTransactionBytes", "secretPhrase");
    }

    @Override
    JSONStreamAware processRequest(HttpServletRequest req) throws NxtException.ValidationException {

        String transactionBytes = Convert.emptyToNull(req.getParameter("unsignedTransactionBytes"));
        if (transactionBytes == null) {
            return MISSING_UNSIGNED_BYTES;
        }
        String secretPhrase = Convert.emptyToNull(req.getParameter("secretPhrase"));
        if (secretPhrase == null) {
            return MISSING_SECRET_PHRASE;
        }

        try {
            byte[] bytes = Convert.parseHexString(transactionBytes);
            Transaction transaction = Nxt.getTransactionProcessor().parseTransaction(bytes);
            transaction.validateAttachment();
            if (transaction.getSignature() != null) {
                JSONObject response = new JSONObject();
                response.put("errorCode", 4);
                response.put("errorDescription", "Incorrect \"unsignedTransactionBytes\" - transaction is already signed");
                return response;
            }
            transaction.sign(secretPhrase);
            JSONObject response = new JSONObject();
            response.put("transaction", transaction.getStringId());
            response.put("fullHash", transaction.getFullHash());
            response.put("transactionBytes", Convert.toHexString(transaction.getBytes()));
            response.put("signatureHash", Convert.toHexString(Crypto.sha256().digest(transaction.getSignature())));
            response.put("verify", transaction.verify());
            return response;
        } catch (NxtException.ValidationException|RuntimeException e) {
            //Logger.logDebugMessage(e.getMessage(), e);
            return INCORRECT_UNSIGNED_BYTES;
        }
    }

}
