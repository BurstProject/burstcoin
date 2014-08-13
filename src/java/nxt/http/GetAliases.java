package nxt.http;

import nxt.Alias;
import nxt.NxtException;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

import javax.servlet.http.HttpServletRequest;

public final class GetAliases extends APIServlet.APIRequestHandler {

    static final GetAliases instance = new GetAliases();

    private GetAliases() {
        super("timestamp", "account");
    }

    @Override
    JSONStreamAware processRequest(HttpServletRequest req) throws NxtException {

        int timestamp = ParameterParser.getTimestamp(req);
        Long accountId = ParameterParser.getAccount(req).getId();

        JSONArray aliases = new JSONArray();
        for (Alias alias : Alias.getAllAliases()) {
            if (alias.getTimestamp() >= timestamp && (accountId == null || alias.getAccount().getId().equals(accountId))) {
                aliases.add(JSONData.alias(alias));
            }
        }

        JSONObject response = new JSONObject();
        response.put("aliases", aliases);
        return response;
    }

}
