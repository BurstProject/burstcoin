package brs.http;

import brs.Account;
import brs.Block;
import brs.Burst;
import brs.NxtException;
import brs.db.NxtIterator;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

import javax.servlet.http.HttpServletRequest;

public final class GetAccountBlockIds extends APIServlet.APIRequestHandler {

  static final GetAccountBlockIds instance = new GetAccountBlockIds();

  private GetAccountBlockIds() {
    super(new APITag[] {APITag.ACCOUNTS}, "account", "timestamp", "firstIndex", "lastIndex");
  }

  @Override
  JSONStreamAware processRequest(HttpServletRequest req) throws NxtException {

    Account account = ParameterParser.getAccount(req);
    int timestamp = ParameterParser.getTimestamp(req);
    int firstIndex = ParameterParser.getFirstIndex(req);
    int lastIndex = ParameterParser.getLastIndex(req);

    JSONArray blockIds = new JSONArray();
    try (NxtIterator<? extends Block> iterator = Burst.getBlockchain().getBlocks(account, timestamp, firstIndex, lastIndex)) {
      while (iterator.hasNext()) {
        Block block = iterator.next();
        blockIds.add(block.getStringId());
      }
    }

    JSONObject response = new JSONObject();
    response.put("blockIds", blockIds);

    return response;
  }

}
