package brs.http;

import static brs.http.common.Parameters.GOODS_PARAMETER;

import brs.BurstException;
import brs.services.ParameterService;
import org.json.simple.JSONStreamAware;

import javax.servlet.http.HttpServletRequest;

public final class GetDGSGood extends APIServlet.APIRequestHandler {

  private final ParameterService parameterService;

  GetDGSGood(ParameterService parameterService) {
    super(new APITag[] {APITag.DGS}, GOODS_PARAMETER);
    this.parameterService = parameterService;
  }

  @Override
  JSONStreamAware processRequest(HttpServletRequest req) throws BurstException {
    return JSONData.goods(parameterService.getGoods(req));
  }

}
