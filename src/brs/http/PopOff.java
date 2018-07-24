package brs.http;

import static brs.http.common.Parameters.HEIGHT_PARAMETER;
import static brs.http.common.Parameters.NUM_BLOCKS_PARAMETER;
import static brs.http.common.ResultFields.BLOCKS_RESPONSE;
import static brs.http.common.ResultFields.ERROR_RESPONSE;

import brs.Block;
import brs.Blockchain;
import brs.BlockchainProcessor;
import brs.services.BlockService;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

import javax.servlet.http.HttpServletRequest;
import java.util.List;

public final class PopOff extends APIServlet.APIRequestHandler {

  private final BlockchainProcessor blockchainProcessor;
  private final Blockchain blockchain;
  private final BlockService blockService;

  PopOff(BlockchainProcessor blockchainProcessor, Blockchain blockchain, BlockService blockService) {
    super(new APITag[] {APITag.DEBUG}, NUM_BLOCKS_PARAMETER, HEIGHT_PARAMETER);
    this.blockchainProcessor = blockchainProcessor;
    this.blockchain = blockchain;
    this.blockService = blockService;
  }

  @Override
  JSONStreamAware processRequest(HttpServletRequest req) {

    JSONObject response = new JSONObject();
    int numBlocks = 0;
    try {
      numBlocks = Integer.parseInt(req.getParameter(NUM_BLOCKS_PARAMETER));
    } catch (NumberFormatException e) {}
    int height = 0;
    try {
      height = Integer.parseInt(req.getParameter(HEIGHT_PARAMETER));
    } catch (NumberFormatException e) {}

    List<? extends Block> blocks;
    JSONArray blocksJSON = new JSONArray();
    if (numBlocks > 0) {
      blocks = blockchainProcessor.popOffTo(blockchain.getHeight() - numBlocks);
    }
    else if (height > 0) {
      blocks = blockchainProcessor.popOffTo(height);
    }
    else {
      response.put(ERROR_RESPONSE, "invalid numBlocks or height");
      return response;
    }
    for (Block block : blocks) {
      blocksJSON.add(JSONData.block(block, true, blockchain.getHeight(), blockService.getBlockReward(block), blockService.getScoopNum(block)));
    }
    response.put(BLOCKS_RESPONSE, blocksJSON);
    return response;
  }

  @Override
  final boolean requirePost() {
    return true;
  }

}
