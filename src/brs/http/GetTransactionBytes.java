package brs.http;

import brs.Blockchain;
import brs.Transaction;
import brs.TransactionProcessor;
import brs.util.Convert;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

import javax.servlet.http.HttpServletRequest;

import static brs.http.JSONResponses.*;
import static brs.http.common.Parameters.TRANSACTION_PARAMETER;

public final class GetTransactionBytes extends APIServlet.APIRequestHandler {

  private final Blockchain blockchain;
  private final TransactionProcessor transactionProcessor;

  GetTransactionBytes(Blockchain blockchain, TransactionProcessor transactionProcessor) {
    super(new APITag[] {APITag.TRANSACTIONS}, TRANSACTION_PARAMETER);
    this.blockchain = blockchain;
    this.transactionProcessor = transactionProcessor;
  }

  @Override
  JSONStreamAware processRequest(HttpServletRequest req) {

    String transactionValue = req.getParameter(TRANSACTION_PARAMETER);
    if (transactionValue == null) {
      return MISSING_TRANSACTION;
    }

    long transactionId;
    Transaction transaction;
    try {
      transactionId = Convert.parseUnsignedLong(transactionValue);
    } catch (RuntimeException e) {
      return INCORRECT_TRANSACTION;
    }

    transaction = blockchain.getTransaction(transactionId);
    JSONObject response = new JSONObject();
    if (transaction == null) {
      transaction = transactionProcessor.getUnconfirmedTransaction(transactionId);
      if (transaction == null) {
        return UNKNOWN_TRANSACTION;
      }
    } else {
      response.put("confirmations", blockchain.getHeight() - transaction.getHeight());
    }

    response.put("transactionBytes", Convert.toHexString(transaction.getBytes()));
    response.put("unsignedTransactionBytes", Convert.toHexString(transaction.getUnsignedBytes()));

    return response;
  }

}
