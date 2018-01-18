package brs.http;

import static brs.http.common.Parameters.ACCOUNT_PARAMETER;
import static brs.http.common.Parameters.NUMBER_OF_CONFIRMATIONS_PARAMETER;
import static brs.http.common.ResultFields.GUARANTEED_BALANCE_NQT;

import brs.Account;
import brs.BurstException;
import brs.services.ParameterService;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

import javax.servlet.http.HttpServletRequest;

public final class GetGuaranteedBalance extends APIServlet.APIRequestHandler {

  private final ParameterService parameterService;

  GetGuaranteedBalance(ParameterService parameterService) {
    super(new APITag[] {APITag.ACCOUNTS, APITag.FORGING}, ACCOUNT_PARAMETER, NUMBER_OF_CONFIRMATIONS_PARAMETER);
    this.parameterService = parameterService;
  }

  @Override
  JSONStreamAware processRequest(HttpServletRequest req) throws BurstException {

    Account account = parameterService.getAccount(req);
    int numberOfConfirmations = parameterService.getNumberOfConfirmations(req);

    JSONObject response = new JSONObject();
    if (account == null) {
      response.put(GUARANTEED_BALANCE_NQT, "0");
    } else {
      response.put(GUARANTEED_BALANCE_NQT, String.valueOf(account.getBalanceNQT()));
    }

    return response;
  }

}
