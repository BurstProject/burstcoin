package brs.http;

import brs.BurstException;
import org.json.simple.JSONStreamAware;

import javax.servlet.http.HttpServletRequest;

public final class GetBalance extends APIServlet.APIRequestHandler {

  static final GetBalance instance = new GetBalance();

  private GetBalance() {
    super(new APITag[] {APITag.ACCOUNTS}, "account");
  }

  @Override
  JSONStreamAware processRequest(HttpServletRequest req) throws BurstException {
    return JSONData.accountBalance(ParameterParser.getAccount(req));
  }

}
