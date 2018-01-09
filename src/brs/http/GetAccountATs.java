package brs.http;

import static brs.http.common.Parameters.ACCOUNT_PARAMETER;

import brs.AT;
import brs.Account;
import brs.BurstException;
import brs.services.ParameterService;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

import javax.servlet.http.HttpServletRequest;
import java.util.List;

public final class GetAccountATs extends APIServlet.APIRequestHandler {

  private final ParameterService parameterService;

  GetAccountATs(ParameterService parameterService) {
    super(new APITag[] {APITag.AT, APITag.ACCOUNTS}, ACCOUNT_PARAMETER);
    this.parameterService = parameterService;
  }
	
  @Override
  JSONStreamAware processRequest(HttpServletRequest req) throws BurstException {
		
    Account account = parameterService.getAccount(req);
		
    List<Long> atIds = AT.getATsIssuedBy(account.getId());
    JSONArray ats = new JSONArray();
    for(long atId : atIds) {
      ats.add(JSONData.at(AT.getAT(atId)));
    }
		
    JSONObject response = new JSONObject();
    response.put("ats", ats);
    return response;
  }
}
