package nxt.peer;

import nxt.Block;
import nxt.Constants;
import nxt.Nxt;
import nxt.util.Convert;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

import java.util.ArrayList;
import java.util.List;

final class GetNextBlocks extends PeerServlet.PeerRequestHandler {

    static final GetNextBlocks instance = new GetNextBlocks();

    private GetNextBlocks() {}


    @Override
    JSONStreamAware processRequest(JSONObject request, Peer peer) {

        JSONObject response = new JSONObject();

        List<Block> nextBlocks = new ArrayList<>();
        int totalLength = 0;
        Long blockId = Convert.parseUnsignedLong((String) request.get("blockId"));
        List<? extends Block> blocks = Nxt.getBlockchain().getBlocksAfter(blockId, 1440);

        for (Block block : blocks) {
            int length = Constants.BLOCK_HEADER_LENGTH + block.getPayloadLength();
            if (totalLength + length > 1048576) {
                break;
            }
            nextBlocks.add(block);
            totalLength += length;
        }

        JSONArray nextBlocksArray = new JSONArray();
        for (Block nextBlock : nextBlocks) {
            nextBlocksArray.add(nextBlock.getJSONObject());
        }
        response.put("nextBlocks", nextBlocksArray);

        return response;
    }

}
