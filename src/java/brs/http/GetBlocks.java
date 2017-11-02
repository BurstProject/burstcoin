package brs.http;

import brs.Block;
import brs.Burst;
import brs.BurstException;
import brs.db.BurstIterator;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

import javax.servlet.http.HttpServletRequest;

public final class GetBlocks extends APIServlet.APIRequestHandler {

  static final GetBlocks instance = new GetBlocks();

  private GetBlocks() {
    super(new APITag[] {APITag.BLOCKS}, "firstIndex", "lastIndex", "includeTransactions");
  }

  @Override
  JSONStreamAware processRequest(HttpServletRequest req) throws BurstException {

    int firstIndex = ParameterParser.getFirstIndex(req);
    int lastIndex = ParameterParser.getLastIndex(req);
    if (lastIndex < 0 || lastIndex - firstIndex > 99) {
      lastIndex = firstIndex + 99;
    }

    boolean includeTransactions = "true".equalsIgnoreCase(req.getParameter("includeTransactions"));

    JSONArray blocks = new JSONArray();
    try (BurstIterator<? extends Block> iterator = Burst.getBlockchain().getBlocks(firstIndex, lastIndex)) {
      while (iterator.hasNext()) {
        Block block = iterator.next();
        blocks.add(JSONData.block(block, includeTransactions));
      }
    }

    JSONObject response = new JSONObject();
    response.put("blocks", blocks);

    return response;
  }

}
