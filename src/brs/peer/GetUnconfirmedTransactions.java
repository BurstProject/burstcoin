package brs.peer;

import static brs.http.common.Parameters.LAST_UNCONFIRMED_TRANSACTION_TIMESTAMP_PARAMETER;
import static brs.http.common.Parameters.LIMIT_UNCONFIRMED_TRANSACTIONS_RETRIEVED_PARAMETER;
import static brs.http.common.ResultFields.LAST_UNCONFIRMED_TRANSACTION_TIMESTAMP_RESPONSE;
import static brs.http.common.ResultFields.UNCONFIRMED_TRANSACTIONS_RESPONSE;

import brs.Transaction;
import brs.TransactionProcessor;
import brs.db.BurstIterator;
import brs.unconfirmedtransactions.TimedUnconfirmedTransactionOverview;
import brs.util.Convert;
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
    Object lastUnconfirmedTransactionTimestampParameter = request.get(LAST_UNCONFIRMED_TRANSACTION_TIMESTAMP_PARAMETER);
    Object limitAmountUnconfirmedTransactionsParameter = request.get(LIMIT_UNCONFIRMED_TRANSACTIONS_RETRIEVED_PARAMETER);

    final Long lastUnconfirmedTransactionTimestamp = lastUnconfirmedTransactionTimestampParameter != null ? Convert.parseLong(lastUnconfirmedTransactionTimestampParameter) : null;
    final Integer limitAmountUnconfirmedTransactions = limitAmountUnconfirmedTransactionsParameter != null ? Convert.parseInteger(limitAmountUnconfirmedTransactionsParameter) : Integer.MAX_VALUE;

    JSONObject response = new JSONObject();

    final TimedUnconfirmedTransactionOverview unconfirmedTransactionsOverview = transactionProcessor.getAllUnconfirmedTransactions(lastUnconfirmedTransactionTimestamp, limitAmountUnconfirmedTransactions);

    JSONArray transactionsData = new JSONArray();
    for (Transaction transaction : unconfirmedTransactionsOverview.getTransactions()) {
      transactionsData.add(transaction.getJSONObject());
    }

    response.put(UNCONFIRMED_TRANSACTIONS_RESPONSE, transactionsData);
    response.put(LAST_UNCONFIRMED_TRANSACTION_TIMESTAMP_RESPONSE, unconfirmedTransactionsOverview.getTimestamp());

    return response;
  }

}
