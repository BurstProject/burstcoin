package brs.peer;

import brs.Account;
import brs.services.AccountService;
import brs.util.Convert;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

public class GetAccountBalance extends PeerServlet.PeerRequestHandler {

  private final AccountService accountService;

  static final String ACCOUNT_ID_PARAMETER_FIELD = "account";
  static final String BALANCE_NQT_RESPONSE_FIELD = "balanceNQT";

  GetAccountBalance(AccountService accountService) {
    this.accountService = accountService;
  }

  @Override
  JSONStreamAware processRequest(JSONObject request, Peer peer) {

    JSONObject response = new JSONObject();

    try {
      Long accountId = Convert.parseAccountId((String) request.get(ACCOUNT_ID_PARAMETER_FIELD));
      Account account = accountService.getAccount(accountId);
      if (account != null) {
        response.put(BALANCE_NQT_RESPONSE_FIELD, Convert.toUnsignedLong(account.getBalanceNQT()));
      } else {
        response.put(BALANCE_NQT_RESPONSE_FIELD, "0");
      }
    } catch (Exception e) {
    }

    return response;
  }
}
