package brs.http;

import static brs.http.common.Parameters.ALIAS_NAME_PARAMETER;
import static brs.http.common.Parameters.ALIAS_PARAMETER;

import brs.Alias;
import brs.services.ParameterService;
import org.json.simple.JSONStreamAware;

import javax.servlet.http.HttpServletRequest;

public final class GetAlias extends APIServlet.APIRequestHandler {

  private final ParameterService parameterService;

  GetAlias(ParameterService parameterService) {
    super(new APITag[] {APITag.ALIASES}, ALIAS_PARAMETER, ALIAS_NAME_PARAMETER);
    this.parameterService = parameterService;
  }

  @Override
  JSONStreamAware processRequest(HttpServletRequest req) throws ParameterException {
    Alias alias = parameterService.getAlias(req);
    return JSONData.alias(alias);
  }

}
