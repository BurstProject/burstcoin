package nxt.http;

import nxt.Nxt;
import nxt.NxtException;
import nxt.Transaction;
import nxt.util.Convert;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

import javax.servlet.http.HttpServletRequest;

import static nxt.http.JSONResponses.INCORRECT_TRANSACTION_BYTES;
import static nxt.http.JSONResponses.MISSING_TRANSACTION_BYTES;

public final class ParseTransaction extends APIServlet.APIRequestHandler {

    static final ParseTransaction instance = new ParseTransaction();

    private ParseTransaction() {
        super("transactionBytes");
    }

    @Override
    JSONStreamAware processRequest(HttpServletRequest req) throws NxtException.ValidationException {

        String transactionBytes = req.getParameter("transactionBytes");
        if (transactionBytes == null) {
            return MISSING_TRANSACTION_BYTES;
        }
        JSONObject response;
        try {
            byte[] bytes = Convert.parseHexString(transactionBytes);
            Transaction transaction = Nxt.getTransactionProcessor().parseTransaction(bytes);
            transaction.validateAttachment();
            response = JSONData.unconfirmedTransaction(transaction);
            response.put("verify", transaction.verify());
        } catch (NxtException.ValidationException|RuntimeException e) {
            //Logger.logDebugMessage(e.getMessage(), e);
            return INCORRECT_TRANSACTION_BYTES;
        }
        return response;
    }

}
