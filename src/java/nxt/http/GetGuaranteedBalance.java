package nxt.http;

import nxt.Account;
import nxt.NxtException;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

import javax.servlet.http.HttpServletRequest;

import static nxt.http.JSONResponses.INCORRECT_NUMBER_OF_CONFIRMATIONS;
import static nxt.http.JSONResponses.MISSING_NUMBER_OF_CONFIRMATIONS;

public final class GetGuaranteedBalance extends APIServlet.APIRequestHandler {

    static final GetGuaranteedBalance instance = new GetGuaranteedBalance();

    private GetGuaranteedBalance() {
        super("account", "numberOfConfirmations");
    }

    @Override
    JSONStreamAware processRequest(HttpServletRequest req) throws NxtException {

        Account account = ParameterParser.getAccount(req);

        String numberOfConfirmationsValue = req.getParameter("numberOfConfirmations");
        if (numberOfConfirmationsValue == null) {
            return MISSING_NUMBER_OF_CONFIRMATIONS;
        }

        JSONObject response = new JSONObject();
        if (account == null) {
            response.put("guaranteedBalanceNQT", "0");
        } else {
            try {
                int numberOfConfirmations = Integer.parseInt(numberOfConfirmationsValue);
                response.put("guaranteedBalanceNQT", String.valueOf(account.getGuaranteedBalanceNQT(numberOfConfirmations)));
            } catch (NumberFormatException e) {
                return INCORRECT_NUMBER_OF_CONFIRMATIONS;
            }
        }

        return response;
    }

}
