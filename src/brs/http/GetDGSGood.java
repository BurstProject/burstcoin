package brs.http;

import brs.BurstException;
import org.json.simple.JSONStreamAware;

import javax.servlet.http.HttpServletRequest;

public final class GetDGSGood extends APIServlet.APIRequestHandler {

  static final GetDGSGood instance = new GetDGSGood();

  private GetDGSGood() {
    super(new APITag[] {APITag.DGS}, "goods");
  }

  @Override
  JSONStreamAware processRequest(HttpServletRequest req) throws BurstException {
    return JSONData.goods(ParameterParser.getGoods(req));
  }

}
