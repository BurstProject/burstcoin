package nxt.http;

import nxt.Account;
import nxt.Nxt;
import nxt.NxtException;
import nxt.db.DbIterator;
import nxt.util.Convert;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

import javax.servlet.http.HttpServletRequest;

public final class GetAccountLessors extends APIServlet.APIRequestHandler {

    static final GetAccountLessors instance = new GetAccountLessors();

    private GetAccountLessors() {
        super(new APITag[] {APITag.ACCOUNTS}, "account", "height");
    }

    @Override
    JSONStreamAware processRequest(HttpServletRequest req) throws NxtException {

        Account account = ParameterParser.getAccount(req);
        int height = ParameterParser.getHeight(req);

        JSONObject response = new JSONObject();
        JSONData.putAccount(response, "account", account.getId());
        response.put("height", height < 0 ? Nxt.getBlockchain().getHeight() : height);
        JSONArray lessorIds = new JSONArray();
        JSONArray lessorIdsRS = new JSONArray();

        try (DbIterator<Account> lessors = account.getLessors(height)) {
            if (lessors.hasNext()) {
                while (lessors.hasNext()) {
                    Account lessor = lessors.next();
                    lessorIds.add(Convert.toUnsignedLong(lessor.getId()));
                    lessorIdsRS.add(Convert.rsAccount(lessor.getId()));
                }
            }
        }
        response.put("lessors", lessorIds);
        response.put("lessorsRS", lessorIdsRS);
        return response;

    }

}
