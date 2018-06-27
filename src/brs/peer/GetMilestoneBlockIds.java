package brs.peer;

import brs.Block;
import brs.Blockchain;
import brs.util.Convert;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

final class GetMilestoneBlockIds extends PeerServlet.PeerRequestHandler {

  private static final Logger logger = LoggerFactory.getLogger(GetMilestoneBlockIds.class);

  private final Blockchain blockchain;

  GetMilestoneBlockIds(Blockchain blockchain) {
    this.blockchain = blockchain;
  }

  @Override
  JSONStreamAware processRequest(JSONObject request, Peer peer) {

    JSONObject response = new JSONObject();
    try {

      JSONArray milestoneBlockIds = new JSONArray();

      String lastBlockIdString = (String) request.get("lastBlockId");
      if (lastBlockIdString != null) {
        long lastBlockId = Convert.parseUnsignedLong(lastBlockIdString);
        long myLastBlockId = blockchain.getLastBlock().getId();
        if (myLastBlockId == lastBlockId || blockchain.hasBlock(lastBlockId)) {
          milestoneBlockIds.add(lastBlockIdString);
          response.put("milestoneBlockIds", milestoneBlockIds);
          if (myLastBlockId == lastBlockId) {
            response.put("last", Boolean.TRUE);
          }
          return response;
        }
      }

      long blockId;
      int height;
      int jump;
      int limit = 10;
      int blockchainHeight = blockchain.getHeight();
      String lastMilestoneBlockIdString = (String) request.get("lastMilestoneBlockId");
      if (lastMilestoneBlockIdString != null) {
        Block lastMilestoneBlock = blockchain.getBlock(Convert.parseUnsignedLong(lastMilestoneBlockIdString));
        if (lastMilestoneBlock == null) {
          throw new IllegalStateException("Don't have block " + lastMilestoneBlockIdString);
        }
        height = lastMilestoneBlock.getHeight();
        jump = Math.min(1440, Math.max(blockchainHeight - height, 1));
        height = Math.max(height - jump, 0);
      } else if (lastBlockIdString != null) {
        height = blockchainHeight;
        jump = 10;
      } else {
        peer.blacklist("GetMilestoneBlockIds");
        response.put("error", "Old getMilestoneBlockIds protocol not supported, please upgrade");
        return response;
      }
      blockId = blockchain.getBlockIdAtHeight(height);

      while (height > 0 && limit-- > 0) {
        milestoneBlockIds.add(Convert.toUnsignedLong(blockId));
        blockId = blockchain.getBlockIdAtHeight(height);
        height = height - jump;
      }
      response.put("milestoneBlockIds", milestoneBlockIds);

    } catch (RuntimeException e) {
      logger.debug(e.toString());
      response.put("error", e.toString());
    }

    return response;
  }

}
