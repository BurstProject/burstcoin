package brs.http;

import static brs.http.common.Parameters.AT_PARAMETER;

import brs.BurstException;
import brs.services.ParameterService;
import javax.servlet.http.HttpServletRequest;
import org.json.simple.JSONStreamAware;

public final class GetAT extends APIServlet.APIRequestHandler {

  private final ParameterService parameterService;

  GetAT(ParameterService parameterService) {
    super(new APITag[]{APITag.AT}, AT_PARAMETER);
    this.parameterService = parameterService;
  }

  @Override
  JSONStreamAware processRequest(HttpServletRequest req) throws BurstException {
    return JSONData.at(parameterService.getAT(req));
  }

}
