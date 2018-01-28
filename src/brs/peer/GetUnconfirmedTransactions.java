package brs.peer;

import brs.Transaction;
import brs.TransactionProcessor;
import brs.db.BurstIterator;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

final class GetUnconfirmedTransactions extends PeerServlet.PeerRequestHandler {

  private final TransactionProcessor transactionProcessor;

  GetUnconfirmedTransactions(TransactionProcessor transactionProcessor) {
    this.transactionProcessor = transactionProcessor;
  }


  @Override
  JSONStreamAware processRequest(JSONObject request, Peer peer) {

    JSONObject response = new JSONObject();

    JSONArray transactionsData = new JSONArray();
    try (BurstIterator<? extends Transaction> transactions = transactionProcessor.getAllUnconfirmedTransactions()) {
      while (transactions.hasNext()) {
        Transaction transaction = transactions.next();
        transactionsData.add(transaction.getJSONObject());
      }
    }

    response.put("unconfirmedTransactions", transactionsData);

    return response;
  }

}
