package brs.http;

import brs.BurstException;
import brs.services.AccountService;
import brs.services.ParameterService;
import org.json.simple.JSONStreamAware;

import javax.servlet.http.HttpServletRequest;

import static brs.http.common.Parameters.AT_PARAMETER;

public class GetATDetails extends APIServlet.APIRequestHandler {

  private final ParameterService parameterService;
  private final AccountService accountService;

  GetATDetails(ParameterService parameterService, AccountService accountService) {
    super(new APITag[] {APITag.AT}, AT_PARAMETER);
    this.parameterService = parameterService;
    this.accountService = accountService;
  }

  @Override
  JSONStreamAware processRequest(HttpServletRequest req) throws BurstException {
    return JSONData.at(parameterService.getAT(req), accountService);
  }
}
