package brs.http;

import brs.BurstException;
import org.json.simple.JSONStreamAware;

import javax.servlet.http.HttpServletRequest;

public final class GetAsset extends APIServlet.APIRequestHandler {

    static final GetAsset instance = new GetAsset();

    private GetAsset() {
        super(new APITag[] {APITag.AE}, "asset");
    }

    @Override
    JSONStreamAware processRequest(HttpServletRequest req) throws BurstException {
        return JSONData.asset(ParameterParser.getAsset(req));
    }

}
