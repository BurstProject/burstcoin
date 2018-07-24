package brs.peer;

import brs.Block;
import brs.Blockchain;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

final class GetCumulativeDifficulty extends PeerServlet.PeerRequestHandler {

  private final Blockchain blockchain;

  GetCumulativeDifficulty(Blockchain blockchain) {
    this.blockchain = blockchain;
  }


  @Override
  JSONStreamAware processRequest(JSONObject request, Peer peer) {
    JSONObject response = new JSONObject();

    Block lastBlock = blockchain.getLastBlock();
    response.put("cumulativeDifficulty", lastBlock.getCumulativeDifficulty().toString());
    response.put("blockchainHeight", lastBlock.getHeight());
    return response;
  }

}
