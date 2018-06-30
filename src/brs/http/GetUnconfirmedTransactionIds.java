package brs.http;

import static brs.http.JSONResponses.INCORRECT_ACCOUNT;
import static brs.http.common.Parameters.ACCOUNT_PARAMETER;
import static brs.http.common.Parameters.LAST_UNCONFIRMED_TRANSACTION_TIMESTAMP_PARAMETER;
import static brs.http.common.ResultFields.LAST_UNCONFIRMED_TRANSACTION_TIMESTAMP_RESPONSE;
import static brs.http.common.ResultFields.UNCONFIRMED_TRANSACTIONS_IDS_RESPONSE;

import brs.Transaction;
import brs.TransactionProcessor;
import brs.unconfirmedtransactions.TimedUnconfirmedTransactionOverview;
import brs.util.Convert;
import javax.servlet.http.HttpServletRequest;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

public final class GetUnconfirmedTransactionIds extends APIServlet.APIRequestHandler {

  private final TransactionProcessor transactionProcessor;

  GetUnconfirmedTransactionIds(TransactionProcessor transactionProcessor) {
    super(new APITag[]{APITag.TRANSACTIONS, APITag.ACCOUNTS}, ACCOUNT_PARAMETER, LAST_UNCONFIRMED_TRANSACTION_TIMESTAMP_PARAMETER);
    this.transactionProcessor = transactionProcessor;
  }

  @Override
  JSONStreamAware processRequest(HttpServletRequest req) {
    final String accountIdString = Convert.emptyToNull(req.getParameter(ACCOUNT_PARAMETER));
    final String lastUnconfirmedTransactionTimestampParameter = Convert.emptyToNull(req.getParameter(LAST_UNCONFIRMED_TRANSACTION_TIMESTAMP_PARAMETER));

    final Long lastUnconfirmedTransactionTimestamp = lastUnconfirmedTransactionTimestampParameter != null ? Long.parseLong(lastUnconfirmedTransactionTimestampParameter) : null;

    long accountId = 0;

    if (accountIdString != null) {
      try {
        accountId = Convert.parseAccountId(accountIdString);
      } catch (RuntimeException e) {
        return INCORRECT_ACCOUNT;
      }
    }

    final JSONArray transactionIds = new JSONArray();

    final TimedUnconfirmedTransactionOverview unconfirmedTransactionOverview = transactionProcessor.getAllUnconfirmedTransactions(lastUnconfirmedTransactionTimestamp);

    for (Transaction transaction : unconfirmedTransactionOverview.getTransactions()) {
      if (accountId != 0 && !(accountId == transaction.getSenderId() || accountId == transaction.getRecipientId())) {
        continue;
      }
      transactionIds.add(transaction.getStringId());
    }

    JSONObject response = new JSONObject();

    response.put(LAST_UNCONFIRMED_TRANSACTION_TIMESTAMP_RESPONSE, unconfirmedTransactionOverview.getTimestamp());
    response.put(UNCONFIRMED_TRANSACTIONS_IDS_RESPONSE, transactionIds);

    return response;
  }

}
