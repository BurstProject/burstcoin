package nxt.http;

import nxt.NxtException;
import nxt.Transaction;
import nxt.util.Convert;
import nxt.util.LoggerConfigurator;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.http.HttpServletRequest;

public final class ParseTransaction extends APIServlet.APIRequestHandler {

    private static final Logger logger = LoggerFactory.getLogger(ParseTransaction.class);

    static final ParseTransaction instance = new ParseTransaction();

    private ParseTransaction() {
        super(new APITag[] {APITag.TRANSACTIONS}, "transactionBytes", "transactionJSON");
    }

    @Override
    JSONStreamAware processRequest(HttpServletRequest req) throws NxtException {

        String transactionBytes = Convert.emptyToNull(req.getParameter("transactionBytes"));
        String transactionJSON = Convert.emptyToNull(req.getParameter("transactionJSON"));
        Transaction transaction = ParameterParser.parseTransaction(transactionBytes, transactionJSON);
        JSONObject response = JSONData.unconfirmedTransaction(transaction);
        try {
            transaction.validate();
        } catch (NxtException.ValidationException|RuntimeException e) {
            logger.debug(e.getMessage(), e);
            response.put("validate", false);
            response.put("errorCode", 4);
            response.put("errorDescription", "Invalid transaction: " + e.toString());
            response.put("error", e.getMessage());
        }
        response.put("verify", transaction.verifySignature() && transaction.verifyPublicKey());
        return response;
    }

}
