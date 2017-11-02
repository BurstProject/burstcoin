package brs.peer;

import brs.Account;
import brs.util.Convert;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

public class GetAccountBalance extends PeerServlet.PeerRequestHandler {
	
	static final GetAccountBalance instance = new GetAccountBalance();
	
	private GetAccountBalance() {}
	
	@Override
	JSONStreamAware processRequest(JSONObject request, Peer peer) {
		
		JSONObject response = new JSONObject();
		
		try {
			Long accountId = Convert.parseAccountId((String)request.get("account"));
			Account account = Account.getAccount(accountId);
			if(account != null) {
				response.put("balanceNQT", Convert.toUnsignedLong(account.getBalanceNQT()));
			}
			else {
				response.put("balanceNQT", "0");
			}
		}
		catch(Exception e) {
		}
		
		return response;
	}

}
