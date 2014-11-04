package nxt.http;

import nxt.Account;
import nxt.Attachment;
import nxt.Constants;
import nxt.Nxt;
import nxt.NxtException;
import nxt.db.DbIterator;
import nxt.util.Convert;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

import javax.servlet.http.HttpServletRequest;

public final class GetAccountsWithRewardRecipient extends APIServlet.APIRequestHandler {
	
	static final GetAccountsWithRewardRecipient instance = new GetAccountsWithRewardRecipient();
	
	private GetAccountsWithRewardRecipient() {
		super(new APITag[] {APITag.ACCOUNTS, APITag.MINING, APITag.INFO}, "account");
	}
	
	@Override
    JSONStreamAware processRequest(HttpServletRequest req) throws NxtException {
		JSONObject response = new JSONObject();
		
		Account targetAccount = ParameterParser.getAccount(req);
		
		long height = Nxt.getBlockchain().getLastBlock().getHeight();
		
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
		DbIterator<Account.RewardRecipientAssignment> assignments = Account.getAccountsWithRewardRecipient(targetAccount.getId());
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
