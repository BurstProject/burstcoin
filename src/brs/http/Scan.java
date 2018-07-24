package brs.http;

import static brs.http.common.Parameters.HEIGHT_PARAMETER;
import static brs.http.common.Parameters.NUM_BLOCKS_PARAMETER;
import static brs.http.common.Parameters.VALIDATE_PARAMETER;
import static brs.http.common.ResultFields.DONE_RESPONSE;
import static brs.http.common.ResultFields.ERROR_RESPONSE;
import static brs.http.common.ResultFields.SCAN_TIME_RESPONSE;

import brs.Blockchain;
import brs.BlockchainProcessor;
import brs.http.common.Parameters;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

import javax.servlet.http.HttpServletRequest;

public final class Scan extends APIServlet.APIRequestHandler {

  private final BlockchainProcessor blockchainProcessor;
  private final Blockchain blockchain;

  Scan(BlockchainProcessor blockchainProcessor, Blockchain blockchain) {
    super(new APITag[] {APITag.DEBUG}, NUM_BLOCKS_PARAMETER, HEIGHT_PARAMETER, VALIDATE_PARAMETER);
    this.blockchainProcessor = blockchainProcessor;
    this.blockchain = blockchain;
  }

  @Override
  JSONStreamAware processRequest(HttpServletRequest req) {
    JSONObject response = new JSONObject();
    try {
      if (Parameters.isTrue(req.getParameter(VALIDATE_PARAMETER))) {
        blockchainProcessor.validateAtNextScan();
      }
      int numBlocks = 0;
      try {
        numBlocks = Integer.parseInt(req.getParameter(NUM_BLOCKS_PARAMETER));
      } catch (NumberFormatException e) {}
      int height = -1;
      try {
        height = Integer.parseInt(req.getParameter(HEIGHT_PARAMETER));
      } catch (NumberFormatException ignore) {}
      long start = System.currentTimeMillis();
      if (numBlocks > 0) {
        blockchainProcessor.scan(blockchain.getHeight() - numBlocks + 1);
      }
      else if (height >= 0) {
        blockchainProcessor.scan(height);
      }
      else {
        response.put(ERROR_RESPONSE, "invalid numBlocks or height");
        return response;
      }
      long end = System.currentTimeMillis();
      response.put(DONE_RESPONSE, true);
      response.put(SCAN_TIME_RESPONSE, (end - start)/1000);
    }
    catch (RuntimeException e) {
      response.put(ERROR_RESPONSE, e.toString());
    }
    return response;
  }

  @Override
  final boolean requirePost() {
    return true;
  }

}
