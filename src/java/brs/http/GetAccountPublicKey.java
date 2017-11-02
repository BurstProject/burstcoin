package brs.http;

import brs.Account;
import brs.BurstException;
import brs.util.Convert;
import brs.util.JSON;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

import javax.servlet.http.HttpServletRequest;

public final class GetAccountPublicKey extends APIServlet.APIRequestHandler {

    static final GetAccountPublicKey instance = new GetAccountPublicKey();

    private GetAccountPublicKey() {
        super(new APITag[] {APITag.ACCOUNTS}, "account");
    }

    @Override
    JSONStreamAware processRequest(HttpServletRequest req) throws BurstException {

        Account account = ParameterParser.getAccount(req);

        if (account.getPublicKey() != null) {
            JSONObject response = new JSONObject();
            response.put("publicKey", Convert.toHexString(account.getPublicKey()));
            return response;
        } else {
            return JSON.emptyJSON;
        }
    }

}
