package nxt.http;

import nxt.util.Convert;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

import javax.servlet.http.HttpServletRequest;

import static nxt.http.JSONResponses.INCORRECT_ACCOUNT;
import static nxt.http.JSONResponses.MISSING_ACCOUNT;

public final class RSConvert extends APIServlet.APIRequestHandler {

    static final RSConvert instance = new RSConvert();

    private RSConvert() {
        super(new APITag[] {APITag.ACCOUNTS, APITag.UTILS}, "account");
    }

    @Override
    JSONStreamAware processRequest(HttpServletRequest req) {
        String accountValue = Convert.emptyToNull(req.getParameter("account"));
        if (accountValue == null) {
            return MISSING_ACCOUNT;
        }
        try {
            long accountId = Convert.parseAccountId(accountValue);
            if (accountId == 0) {
                return INCORRECT_ACCOUNT;
            }
            JSONObject response = new JSONObject();
            JSONData.putAccount(response, "account", accountId);
            return response;
        } catch (RuntimeException e) {
            return INCORRECT_ACCOUNT;
        }
    }

}
