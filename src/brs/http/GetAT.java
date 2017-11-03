package brs.http;

import brs.BurstException;
import org.json.simple.JSONStreamAware;

import javax.servlet.http.HttpServletRequest;

public final class GetAT extends APIServlet.APIRequestHandler {

    static final GetAT instance = new GetAT();

    private GetAT() {
        super(new APITag[] {APITag.AT}, "at");
    }

    @Override
    JSONStreamAware processRequest(HttpServletRequest req) throws BurstException {
        return JSONData.at(ParameterParser.getAT(req));
    }

}
