package nxt.peer;

import nxt.Account;
import nxt.Nxt;
import nxt.Transaction;
import nxt.util.Convert;
import nxt.db.DbIterator;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

public class GetAccountRecentTransactions extends PeerServlet.PeerRequestHandler {
	
	static final GetAccountRecentTransactions instance = new GetAccountRecentTransactions();
	
	private GetAccountRecentTransactions() {}
	
	@Override
	JSONStreamAware processRequest(JSONObject request, Peer peer) {
		
		JSONObject response = new JSONObject();
		
		try {
			Long accountId = Convert.parseAccountId((String)request.get("account"));
			Account account = Account.getAccount(accountId);
			JSONArray transactions = new JSONArray();
			if(account != null) {
				DbIterator<? extends Transaction> iterator = Nxt.getBlockchain().getTransactions(account, 0, (byte)-1, (byte)0, 0, 0, 9);
				while(iterator.hasNext()) {
					Transaction transaction = iterator.next();
					transactions.add(nxt.http.JSONData.transaction(transaction));
				}
			}
			response.put("transactions", transactions);
		}
		catch(Exception e) {
		}
		
		return response;
	}

}
