package brs.http;

import static brs.http.common.Parameters.TIMESTAMP_PARAMETER;

import brs.Block;
import brs.Blockchain;
import brs.EconomicClustering;
import brs.Burst;
import brs.BurstException;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

import javax.servlet.http.HttpServletRequest;

public final class GetECBlock extends APIServlet.APIRequestHandler {

  private final Blockchain blockchain;

  GetECBlock(Blockchain blockchain) {
    super(new APITag[] {APITag.BLOCKS}, TIMESTAMP_PARAMETER);
    this.blockchain = blockchain;
  }

  @Override
  JSONStreamAware processRequest(HttpServletRequest req) throws BurstException {
    int timestamp = ParameterParser.getTimestamp(req);
    if (timestamp == 0) {
      timestamp = Burst.getEpochTime();
    }
    if (timestamp < blockchain.getLastBlock().getTimestamp() - 15) {
      return JSONResponses.INCORRECT_TIMESTAMP;
    }
    Block ecBlock = EconomicClustering.getECBlock(timestamp);
    JSONObject response = new JSONObject();
    response.put("ecBlockId", ecBlock.getStringId());
    response.put("ecBlockHeight", ecBlock.getHeight());
    response.put("timestamp", timestamp);
    return response;
  }

}
