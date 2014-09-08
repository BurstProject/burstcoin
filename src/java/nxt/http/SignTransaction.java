package nxt.http;

import nxt.Nxt;
import nxt.NxtException;
import nxt.Transaction;
import nxt.crypto.Crypto;
import nxt.util.Convert;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;
import org.json.simple.JSONValue;

import javax.servlet.http.HttpServletRequest;

import static nxt.http.JSONResponses.INCORRECT_UNSIGNED_BYTES;
import static nxt.http.JSONResponses.MISSING_SECRET_PHRASE;
import static nxt.http.JSONResponses.MISSING_UNSIGNED_BYTES;

public final class SignTransaction extends APIServlet.APIRequestHandler {

    static final SignTransaction instance = new SignTransaction();

    private SignTransaction() {
        super(new APITag[] {APITag.TRANSACTIONS}, "unsignedTransactionBytes", "unsignedTransactionJSON", "secretPhrase");
    }

    @Override
    JSONStreamAware processRequest(HttpServletRequest req) throws NxtException.ValidationException {

        String transactionBytes = Convert.emptyToNull(req.getParameter("unsignedTransactionBytes"));
        String transactionJSON = Convert.emptyToNull(req.getParameter("unsignedTransactionJSON"));
        if (transactionBytes == null && transactionJSON == null) {
            return MISSING_UNSIGNED_BYTES;
        }
        String secretPhrase = Convert.emptyToNull(req.getParameter("secretPhrase"));
        if (secretPhrase == null) {
            return MISSING_SECRET_PHRASE;
        }

        try {
            Transaction transaction;
            if (transactionBytes != null) {
                byte[] bytes = Convert.parseHexString(transactionBytes);
                transaction = Nxt.getTransactionProcessor().parseTransaction(bytes);
            } else {
                JSONObject json = (JSONObject) JSONValue.parse(transactionJSON);
                transaction = Nxt.getTransactionProcessor().parseTransaction(json);
            }
            transaction.validate();
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
            response.put("verify", transaction.verifySignature());
            return response;
        } catch (NxtException.ValidationException|RuntimeException e) {
            //Logger.logDebugMessage(e.getMessage(), e);
            return INCORRECT_UNSIGNED_BYTES;
        }
    }

}
