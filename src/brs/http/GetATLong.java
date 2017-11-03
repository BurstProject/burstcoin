package brs.http;

import brs.BurstException;
import org.json.simple.JSONStreamAware;

import javax.servlet.http.HttpServletRequest;

public final class GetATLong extends APIServlet.APIRequestHandler {

    static final GetATLong instance = new GetATLong();

    private GetATLong() {
        super(new APITag[] {APITag.AT}, "hexString");
    }

    @Override
    JSONStreamAware processRequest(HttpServletRequest req) throws BurstException {
        return JSONData.hex2long(ParameterParser.getATLong(req));
    }
    
    

}
