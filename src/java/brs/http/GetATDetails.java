package brs.http;

import brs.BurstException;
import org.json.simple.JSONStreamAware;

import javax.servlet.http.HttpServletRequest;

public class GetATDetails extends APIServlet.APIRequestHandler {
	static final GetATDetails instance = new GetATDetails();

    private GetATDetails() {
        super(new APITag[] {APITag.AT}, "at");
    }

    @Override
    JSONStreamAware processRequest(HttpServletRequest req) throws BurstException {
        return JSONData.at(ParameterParser.getAT(req));
    }
}
