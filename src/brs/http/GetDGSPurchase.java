package brs.http;

import brs.BurstException;
import org.json.simple.JSONStreamAware;

import javax.servlet.http.HttpServletRequest;

import static brs.http.common.Parameters.PURCHASE_PARAMETER;

public final class GetDGSPurchase extends APIServlet.APIRequestHandler {

  static final GetDGSPurchase instance = new GetDGSPurchase();

  private GetDGSPurchase() {
    super(new APITag[] {APITag.DGS}, PURCHASE_PARAMETER);
  }

  @Override
  JSONStreamAware processRequest(HttpServletRequest req) throws BurstException {
    return JSONData.purchase(ParameterParser.getPurchase(req));
  }

}
