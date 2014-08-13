package nxt.http;

import nxt.Nxt;
import nxt.Transaction;
import nxt.util.Convert;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

import javax.servlet.http.HttpServletRequest;

import static nxt.http.JSONResponses.INCORRECT_ACCOUNT;

public final class GetUnconfirmedTransactionIds extends APIServlet.APIRequestHandler {

    static final GetUnconfirmedTransactionIds instance = new GetUnconfirmedTransactionIds();

    private GetUnconfirmedTransactionIds() {
        super("account");
    }

    @Override
    JSONStreamAware processRequest(HttpServletRequest req) {

        String accountIdString = Convert.emptyToNull(req.getParameter("account"));
        Long accountId = null;

        if (accountIdString != null) {
            try {
                accountId = Convert.parseUnsignedLong(accountIdString);
            } catch (RuntimeException e) {
                return INCORRECT_ACCOUNT;
            }
        }

        JSONArray transactionIds = new JSONArray();
        for (Transaction transaction : Nxt.getTransactionProcessor().getAllUnconfirmedTransactions()) {
            if (accountId != null && ! (accountId.equals(transaction.getSenderId()) || accountId.equals(transaction.getRecipientId()))) {
                continue;
            }
            transactionIds.add(transaction.getStringId());
        }

        JSONObject response = new JSONObject();
        response.put("unconfirmedTransactionIds", transactionIds);
        return response;
    }

}
