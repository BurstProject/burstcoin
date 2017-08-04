package nxt.http;

import nxt.AT;
import nxt.util.Convert;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

import javax.servlet.http.HttpServletRequest;

public final class GetATIds extends APIServlet.APIRequestHandler {

    static final GetATIds instance = new GetATIds();

    private GetATIds() {
        super(new APITag[] {APITag.AT});
    }

    @Override
    JSONStreamAware processRequest(HttpServletRequest req) {

        JSONArray atIds = new JSONArray();
        for (Long id : AT.getAllATIds()) {
            atIds.add(Convert.toUnsignedLong(id));
        }

        JSONObject response = new JSONObject();
        response.put("atIds", atIds);
        return response;
    }

}