package brs.http;

import static brs.http.common.Parameters.HEIGHT_PARAMETER;
import static brs.http.common.Parameters.NUM_BLOCKS_PARAMETER;

import brs.Block;
import brs.BlockchainProcessor;
import brs.Burst;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

import javax.servlet.http.HttpServletRequest;
import java.util.List;

public final class PopOff extends APIServlet.APIRequestHandler {

  private final BlockchainProcessor blockchainProcessor;

  PopOff(BlockchainProcessor blockchainProcessor) {
    super(new APITag[] {APITag.DEBUG}, NUM_BLOCKS_PARAMETER, HEIGHT_PARAMETER);
    this.blockchainProcessor = blockchainProcessor;
  }

  @Override
  JSONStreamAware processRequest(HttpServletRequest req) {

    JSONObject response = new JSONObject();
    int numBlocks = 0;
    try {
      numBlocks = Integer.parseInt(req.getParameter("numBlocks"));
    } catch (NumberFormatException e) {}
    int height = 0;
    try {
      height = Integer.parseInt(req.getParameter("height"));
    } catch (NumberFormatException e) {}

    List<? extends Block> blocks;
    JSONArray blocksJSON = new JSONArray();
    if (numBlocks > 0) {
      blocks = blockchainProcessor.popOffTo(Burst.getBlockchain().getHeight() - numBlocks);
    }
    else if (height > 0) {
      blocks = blockchainProcessor.popOffTo(height);
    }
    else {
      response.put("error", "invalid numBlocks or height");
      return response;
    }
    for (Block block : blocks) {
      blocksJSON.add(JSONData.block(block, true));
    }
    response.put("blocks", blocksJSON);
    return response;
  }

  @Override
  final boolean requirePost() {
    return true;
  }

}
