package brs.peer;

import brs.Block;
import brs.Burst;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

final class GetCumulativeDifficulty extends PeerServlet.PeerRequestHandler {

  static final GetCumulativeDifficulty instance = new GetCumulativeDifficulty();

  private GetCumulativeDifficulty() {}


  @Override
  JSONStreamAware processRequest(JSONObject request, Peer peer) {

    JSONObject response = new JSONObject();

    Block lastBlock = Burst.getBlockchain().getLastBlock();
    response.put("cumulativeDifficulty", lastBlock.getCumulativeDifficulty().toString());
    response.put("blockchainHeight", lastBlock.getHeight());
    return response;
  }

}
