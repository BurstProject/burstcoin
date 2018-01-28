package brs.peer;

import brs.Account;
import brs.Blockchain;
import brs.Transaction;
import brs.db.BurstIterator;
import brs.util.Convert;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

public class GetAccountRecentTransactions extends PeerServlet.PeerRequestHandler {
	
  private final Blockchain blockchain;
	
  GetAccountRecentTransactions(Blockchain blockchain) {
    this.blockchain = blockchain;
  }
	
  @Override
  JSONStreamAware processRequest(JSONObject request, Peer peer) {
		
    JSONObject response = new JSONObject();
		
    try {
      Long accountId = Convert.parseAccountId((String)request.get("account"));
      Account account = Account.getAccount(accountId);
      JSONArray transactions = new JSONArray();
      if(account != null) {
        BurstIterator<? extends Transaction> iterator = blockchain.getTransactions(account, 0, (byte)-1, (byte)0, 0, 0, 9);
        while(iterator.hasNext()) {
          Transaction transaction = iterator.next();
          transactions.add(brs.http.JSONData.transaction(transaction, blockchain.getHeight()));
        }
      }
      response.put("transactions", transactions);
    }
    catch(Exception e) {
    }
		
    return response;
  }

}
