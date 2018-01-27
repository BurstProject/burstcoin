package brs.http;

import static brs.http.common.Parameters.ACCOUNT_PARAMETER;
import static brs.http.common.Parameters.FIRST_INDEX_PARAMETER;
import static brs.http.common.Parameters.INCLUDE_TRANSACTIONS_PARAMETER;
import static brs.http.common.Parameters.LAST_INDEX_PARAMETER;
import static brs.http.common.Parameters.TIMESTAMP_PARAMETER;
import static brs.http.common.ResultFields.BLOCKS_RESPONSE;

import brs.Account;
import brs.Block;
import brs.Blockchain;
import brs.BurstException;
import brs.db.BurstIterator;
import brs.http.common.Parameters;
import brs.services.ParameterService;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

import javax.servlet.http.HttpServletRequest;

public final class GetAccountBlocks extends APIServlet.APIRequestHandler {

  private final Blockchain blockchain;
  private final ParameterService parameterService;

  GetAccountBlocks(Blockchain blockchain, ParameterService parameterService) {
    super(new APITag[] {APITag.ACCOUNTS}, ACCOUNT_PARAMETER, TIMESTAMP_PARAMETER, FIRST_INDEX_PARAMETER, LAST_INDEX_PARAMETER, INCLUDE_TRANSACTIONS_PARAMETER);
    this.blockchain = blockchain;
    this.parameterService = parameterService;
  }

  @Override
  JSONStreamAware processRequest(HttpServletRequest req) throws BurstException {

    Account account = parameterService.getAccount(req);
    int timestamp = ParameterParser.getTimestamp(req);
    int firstIndex = ParameterParser.getFirstIndex(req);
    int lastIndex = ParameterParser.getLastIndex(req);

    boolean includeTransactions = Parameters.isTrue(req.getParameter(INCLUDE_TRANSACTIONS_PARAMETER));

    JSONArray blocks = new JSONArray();
    try (BurstIterator<? extends Block> iterator = blockchain.getBlocks(account, timestamp, firstIndex, lastIndex)) {
      while (iterator.hasNext()) {
        Block block = iterator.next();
        blocks.add(JSONData.block(block, includeTransactions, blockchain.getHeight()));
      }
    }

    JSONObject response = new JSONObject();
    response.put(BLOCKS_RESPONSE, blocks);

    return response;
  }

}
