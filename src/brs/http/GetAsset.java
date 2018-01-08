package brs.http;

import static brs.http.common.Parameters.ASSET_PARAMETER;

import brs.BurstException;
import brs.services.ParameterService;
import javax.servlet.http.HttpServletRequest;
import org.json.simple.JSONStreamAware;

public final class GetAsset extends APIServlet.APIRequestHandler {

  private final ParameterService parameterService;

  GetAsset(ParameterService parameterService) {
    super(new APITag[]{APITag.AE}, ASSET_PARAMETER);
    this.parameterService = parameterService;
  }

  @Override
  JSONStreamAware processRequest(HttpServletRequest req) throws BurstException {
    return JSONData.asset(parameterService.getAsset(req));
  }

}
