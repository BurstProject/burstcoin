package nxt.http;

import nxt.Nxt;
import nxt.NxtException;
import nxt.Transaction;
import nxt.util.Convert;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;
import org.json.simple.JSONValue;

import javax.servlet.http.HttpServletRequest;

import static nxt.http.JSONResponses.INCORRECT_TRANSACTION_BYTES;
import static nxt.http.JSONResponses.MISSING_TRANSACTION_BYTES_OR_JSON;

public final class BroadcastTransaction extends APIServlet.APIRequestHandler {

    static final BroadcastTransaction instance = new BroadcastTransaction();

    private BroadcastTransaction() {
        super(new APITag[] {APITag.TRANSACTIONS}, "transactionBytes", "transactionJSON");
    }

    @Override
    JSONStreamAware processRequest(HttpServletRequest req) throws NxtException.ValidationException {

        String transactionBytes = Convert.emptyToNull(req.getParameter("transactionBytes"));
        String transactionJSON = Convert.emptyToNull(req.getParameter("transactionJSON"));
        if (transactionBytes == null && transactionJSON == null) {
            return MISSING_TRANSACTION_BYTES_OR_JSON;
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

            JSONObject response = new JSONObject();

            try {
                Nxt.getTransactionProcessor().broadcast(transaction);
                response.put("transaction", transaction.getStringId());
                response.put("fullHash", transaction.getFullHash());
            } catch (NxtException.ValidationException e) {
                response.put("error", e.getMessage());
            }

            return response;

        } catch (RuntimeException e) {
            return INCORRECT_TRANSACTION_BYTES;
        }
    }

    @Override
    boolean requirePost() {
        return true;
    }

}
