package nxt.http;

import nxt.NxtException;
import org.json.simple.JSONStreamAware;

import javax.servlet.http.HttpServletRequest;

public final class GetAT extends APIServlet.APIRequestHandler {

    static final GetAT instance = new GetAT();

    private GetAT() {
        super(new APITag[] {APITag.AT}, "at");
    }

    @Override
    JSONStreamAware processRequest(HttpServletRequest req) throws NxtException {
        return JSONData.at(ParameterParser.getAT(req));
    }

}
