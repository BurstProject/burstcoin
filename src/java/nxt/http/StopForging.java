package nxt.http;

import nxt.Generator;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

import javax.servlet.http.HttpServletRequest;

import static nxt.http.JSONResponses.MISSING_SECRET_PHRASE;


public final class StopForging extends APIServlet.APIRequestHandler {

    static final StopForging instance = new StopForging();

    private StopForging() {
        super("secretPhrase");
    }

    @Override
    JSONStreamAware processRequest(HttpServletRequest req) {

        String secretPhrase = req.getParameter("secretPhrase");
        if (secretPhrase == null) {
            return MISSING_SECRET_PHRASE;
        }

        Generator generator = Generator.stopForging(secretPhrase);

        JSONObject response = new JSONObject();
        response.put("foundAndStopped", generator != null);
        return response;

    }

    @Override
    boolean requirePost() {
        return true;
    }

}
