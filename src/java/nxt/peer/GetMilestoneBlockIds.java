package nxt.peer;

import nxt.Block;
import nxt.Nxt;
import nxt.util.Convert;
import nxt.util.Logger;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

final class GetMilestoneBlockIds extends PeerServlet.PeerRequestHandler {

    static final GetMilestoneBlockIds instance = new GetMilestoneBlockIds();

    private GetMilestoneBlockIds() {}


    @Override
    JSONStreamAware processRequest(JSONObject request, Peer peer) {

        JSONObject response = new JSONObject();
        try {

            JSONArray milestoneBlockIds = new JSONArray();

            String lastBlockIdString = (String) request.get("lastBlockId");
            if (lastBlockIdString != null) {
                Long lastBlockId = Convert.parseUnsignedLong(lastBlockIdString);
                Long myLastBlockId = Nxt.getBlockchain().getLastBlock().getId();
                if (myLastBlockId.equals(lastBlockId) || Nxt.getBlockchain().hasBlock(lastBlockId)) {
                    milestoneBlockIds.add(lastBlockIdString);
                    response.put("milestoneBlockIds", milestoneBlockIds);
                    if (myLastBlockId.equals(lastBlockId)) {
                        response.put("last", Boolean.TRUE);
                    }
                    return response;
                }
            }

            long blockId;
            int height;
            int jump;
            int limit;
            String lastMilestoneBlockIdString = (String) request.get("lastMilestoneBlockId");
            if (lastMilestoneBlockIdString != null) {
                Block lastMilestoneBlock = Nxt.getBlockchain().getBlock(Convert.parseUnsignedLong(lastMilestoneBlockIdString));
                if (lastMilestoneBlock == null) {
                    throw new IllegalStateException("Don't have block " + lastMilestoneBlockIdString);
                }
                height = lastMilestoneBlock.getHeight();
                jump = Math.min(1440, Nxt.getBlockchain().getLastBlock().getHeight() - height);
                height = Math.max(height - jump, 0);
                limit = 10;
            } else if (lastBlockIdString != null) {
                height = Nxt.getBlockchain().getLastBlock().getHeight();
                jump = 10;
                limit = 10;
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
            Logger.logDebugMessage(e.toString());
            response.put("error", e.toString());
        }

        return response;
    }

}
