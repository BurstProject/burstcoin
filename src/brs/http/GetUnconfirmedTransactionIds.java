package brs.http;

import brs.Transaction;
import brs.TransactionProcessor;
import brs.db.BurstIterator;
import brs.util.Convert;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

import javax.servlet.http.HttpServletRequest;

import static brs.http.JSONResponses.INCORRECT_ACCOUNT;
import static brs.http.common.Parameters.ACCOUNT_PARAMETER;

public final class GetUnconfirmedTransactionIds extends APIServlet.APIRequestHandler {

  private final TransactionProcessor transactionProcessor;

  GetUnconfirmedTransactionIds(TransactionProcessor transactionProcessor) {
    super(new APITag[] {APITag.TRANSACTIONS, APITag.ACCOUNTS}, ACCOUNT_PARAMETER);
    this.transactionProcessor = transactionProcessor;
  }

  @Override
  JSONStreamAware processRequest(HttpServletRequest req) {

    String accountIdString = Convert.emptyToNull(req.getParameter(ACCOUNT_PARAMETER));
    long accountId = 0;

    if (accountIdString != null) {
      try {
        accountId = Convert.parseAccountId(accountIdString);
      } catch (RuntimeException e) {
        return INCORRECT_ACCOUNT;
      }
    }

    JSONArray transactionIds = new JSONArray();
    try (BurstIterator<? extends Transaction> transactionsIterator = transactionProcessor.getAllUnconfirmedTransactions()) {
      while (transactionsIterator.hasNext()) {
        Transaction transaction = transactionsIterator.next();
        if (accountId != 0 && !(accountId == transaction.getSenderId() || accountId == transaction.getRecipientId())) {
          continue;
        }
        transactionIds.add(transaction.getStringId());
      }
    }

    JSONObject response = new JSONObject();
    response.put("unconfirmedTransactionIds", transactionIds);
    return response;
  }

}
