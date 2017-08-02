package nxt.peer;

import nxt.Block;
import nxt.Nxt;
import nxt.util.Convert;
import nxt.util.LoggerConfigurator;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

final class GetMilestoneBlockIds extends PeerServlet.PeerRequestHandler {

    private static final Logger logger = LoggerFactory.getLogger(GetMilestoneBlockIds.class);

    static final GetMilestoneBlockIds instance = new GetMilestoneBlockIds();

    private GetMilestoneBlockIds() {}


    @Override
    JSONStreamAware processRequest(JSONObject request, Peer peer) {

        JSONObject response = new JSONObject();
        try {

            JSONArray milestoneBlockIds = new JSONArray();

            String lastBlockIdString = (String) request.get("lastBlockId");
            if (lastBlockIdString != null) {
                long lastBlockId = Convert.parseUnsignedLong(lastBlockIdString);
                long myLastBlockId = Nxt.getBlockchain().getLastBlock().getId();
                if (myLastBlockId == lastBlockId || Nxt.getBlockchain().hasBlock(lastBlockId)) {
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
            int blockchainHeight = Nxt.getBlockchain().getHeight();
            String lastMilestoneBlockIdString = (String) request.get("lastMilestoneBlockId");
            if (lastMilestoneBlockIdString != null) {
                Block lastMilestoneBlock = Nxt.getBlockchain().getBlock(Convert.parseUnsignedLong(lastMilestoneBlockIdString));
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
                peer.blacklist();
                response.put("error", "Old getMilestoneBlockIds protocol not supported, please upgrade");
                return response;
            }
            blockId = Nxt.getBlockchain().getBlockIdAtHeight(height);

            while (height > 0 && limit-- > 0) {
                milestoneBlockIds.add(Convert.toUnsignedLong(blockId));
                blockId = Nxt.getBlockchain().getBlockIdAtHeight(height);
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
