package brs.http;

import static brs.http.common.Parameters.ACCOUNT_PARAMETER;

import brs.Account;
import brs.Burst;
import brs.BurstException;
import brs.db.BurstIterator;
import brs.services.AccountService;
import brs.services.ParameterService;
import brs.util.Convert;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

import javax.servlet.http.HttpServletRequest;

public final class GetAccountsWithRewardRecipient extends APIServlet.APIRequestHandler {

  private final ParameterService parameterService;
  private final AccountService accountService;

  GetAccountsWithRewardRecipient(ParameterService parameterService, AccountService accountService) {
    super(new APITag[] {APITag.ACCOUNTS, APITag.MINING, APITag.INFO}, ACCOUNT_PARAMETER);
    this.parameterService = parameterService;
    this.accountService = accountService;
  }
	
  @Override
  JSONStreamAware processRequest(HttpServletRequest req) throws BurstException {
    JSONObject response = new JSONObject();
		
    Account targetAccount = parameterService.getAccount(req);
		
    long height = Burst.getBlockchain().getLastBlock().getHeight();
		
    JSONArray accounts = new JSONArray();
    /*for(Account account : Account.getAllAccounts()) {
      long recip;
      if(account.getRewardRecipientFrom() > height + 1) {
      recip = 0L; // this api is intended for pools, so drop changing users a few blocks early to avoid overpaying
      }
      else {
      recip = account.getRewardRecipient();
      }
      if(targetAccount.getId() == recip) {
      accounts.add(Convert.toUnsignedLong(account.getId()));
      }
      }*/
    BurstIterator<Account.RewardRecipientAssignment> assignments = accountService.getAccountsWithRewardRecipient(targetAccount.getId());
    while(assignments.hasNext()) {
      Account.RewardRecipientAssignment assignment = assignments.next();
      accounts.add(Convert.toUnsignedLong(assignment.accountId));
    }
    if(targetAccount.getRewardRecipientAssignment() == null) {
      accounts.add(Convert.toUnsignedLong(targetAccount.getId()));
    }
		
    response.put("accounts", accounts);
		
    return response;
  }
}
