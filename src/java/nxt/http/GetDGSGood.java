package nxt.http;

import nxt.NxtException;
import org.json.simple.JSONStreamAware;

import javax.servlet.http.HttpServletRequest;

public final class GetDGSGood extends APIServlet.APIRequestHandler {

    static final GetDGSGood instance = new GetDGSGood();

    private GetDGSGood() {
        super(new APITag[] {APITag.DGS}, "goods");
    }

    @Override
    JSONStreamAware processRequest(HttpServletRequest req) throws NxtException {
        return JSONData.goods(ParameterParser.getGoods(req));
    }

}
