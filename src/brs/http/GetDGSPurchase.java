package brs.http;

import brs.BurstException;
import brs.services.ParameterService;
import org.json.simple.JSONStreamAware;

import javax.servlet.http.HttpServletRequest;

import static brs.http.common.Parameters.PURCHASE_PARAMETER;

public final class GetDGSPurchase extends APIServlet.APIRequestHandler {

  private final ParameterService parameterService;

  public GetDGSPurchase(ParameterService parameterService) {
    super(new APITag[] {APITag.DGS}, PURCHASE_PARAMETER);
    this.parameterService = parameterService;
  }

  @Override
  JSONStreamAware processRequest(HttpServletRequest req) throws BurstException {
    return JSONData.purchase(parameterService.getPurchase(req));
  }

}
