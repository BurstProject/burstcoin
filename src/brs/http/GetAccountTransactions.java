package brs.http;

import static brs.http.common.Parameters.ACCOUNT_PARAMETER;
import static brs.http.common.Parameters.FIRST_INDEX_PARAMETER;
import static brs.http.common.Parameters.LAST_INDEX_PARAMETER;
import static brs.http.common.Parameters.NUMBER_OF_CONFIRMATIONS_PARAMETER;
import static brs.http.common.Parameters.SUBTYPE_PARAMETER;
import static brs.http.common.Parameters.TIMESTAMP_PARAMETER;
import static brs.http.common.Parameters.TYPE_PARAMETER;
import static brs.http.common.ResultFields.TRANSACTIONS_RESPONSE;

import brs.Account;
import brs.Blockchain;
import brs.Burst;
import brs.BurstException;
import brs.Transaction;
import brs.db.BurstIterator;
import brs.services.ParameterService;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;
import java.lang.Math;
import javax.servlet.http.HttpServletRequest;

public final class GetAccountTransactions extends APIServlet.APIRequestHandler {

  private final ParameterService parameterService;
  private final Blockchain blockchain;

  GetAccountTransactions(ParameterService parameterService, Blockchain blockchain) {
    super(new APITag[] {APITag.ACCOUNTS}, ACCOUNT_PARAMETER, TIMESTAMP_PARAMETER, TYPE_PARAMETER, SUBTYPE_PARAMETER, FIRST_INDEX_PARAMETER, LAST_INDEX_PARAMETER, NUMBER_OF_CONFIRMATIONS_PARAMETER);
    this.parameterService = parameterService;
    this.blockchain = blockchain;
  }

  @Override
  JSONStreamAware processRequest(HttpServletRequest req) throws BurstException {
    Account account = parameterService.getAccount(req);
    int timestamp = ParameterParser.getTimestamp(req);
    int numberOfConfirmations = parameterService.getNumberOfConfirmations(req);

    byte type;
    byte subtype;
    try {
      type = Byte.parseByte(req.getParameter(TYPE_PARAMETER));
    }
    catch (NumberFormatException e) {
      type = -1;
    }
    try {
      subtype = Byte.parseByte(req.getParameter(SUBTYPE_PARAMETER));
    }
    catch (NumberFormatException e) {
      subtype = -1;
    }

    int maxIndex   = Burst.getIntProperty("brs.apiMaxIndex"); // maxIndex as defined in config (if any)
    int firstIndex = ParameterParser.getFirstIndex(req);
    int lastIndex  = ParameterParser.getLastIndex(req);

    if (maxIndex > 0) {                                       // only if limit has been defined
      lastIndex = Math.min(lastIndex, maxIndex);              // take the lower of the two
    }

    System.out.println("first: " + firstIndex + " last: " + lastIndex + "\n");

    JSONArray transactions = new JSONArray();
    try (BurstIterator<? extends Transaction> iterator = blockchain.getTransactions(account, numberOfConfirmations, type, subtype, timestamp,
                                                                                               firstIndex, lastIndex)) {
      while (iterator.hasNext()) {
        Transaction transaction = iterator.next();
        transactions.add(JSONData.transaction(transaction, blockchain.getHeight()));
      }
    }

    JSONObject response = new JSONObject();
    response.put(TRANSACTIONS_RESPONSE, transactions);
    return response;

  }

}
