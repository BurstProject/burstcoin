package nxt.peer;

import nxt.Nxt;
import nxt.util.Convert;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

import java.util.List;

final class GetNextBlockIds extends PeerServlet.PeerRequestHandler {

    static final GetNextBlockIds instance = new GetNextBlockIds();

    private GetNextBlockIds() {}


    @Override
    JSONStreamAware processRequest(JSONObject request, Peer peer) {

        JSONObject response = new JSONObject();

        JSONArray nextBlockIds = new JSONArray();
        Long blockId = Convert.parseUnsignedLong((String) request.get("blockId"));
        List<Long> ids = Nxt.getBlockchain().getBlockIdsAfter(blockId, 1440);

        for (Long id : ids) {
            nextBlockIds.add(Convert.toUnsignedLong(id));
        }

        response.put("nextBlockIds", nextBlockIds);

        return response;
    }

}
