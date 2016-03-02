package nxt.http;

import nxt.Block;
import nxt.Nxt;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

import javax.servlet.http.HttpServletRequest;
import java.util.List;

public final class PopOff extends APIServlet.APIRequestHandler {

    static final PopOff instance = new PopOff();

    private PopOff() {
        super(new APITag[] {APITag.DEBUG}, "numBlocks", "height");
    }

    @Override
    JSONStreamAware processRequest(HttpServletRequest req) {

        JSONObject response = new JSONObject();
        int numBlocks = 0;
        try {
            numBlocks = Integer.parseInt(req.getParameter("numBlocks"));
        } catch (NumberFormatException e) {}
        int height = 0;
        try {
            height = Integer.parseInt(req.getParameter("height"));
        } catch (NumberFormatException e) {}

        List<? extends Block> blocks;
        JSONArray blocksJSON = new JSONArray();
        if (numBlocks > 0) {
            blocks = Nxt.getBlockchainProcessor().popOffTo(Nxt.getBlockchain().getHeight() - numBlocks);
        } else if (height > 0) {
            blocks = Nxt.getBlockchainProcessor().popOffTo(height);
        } else {
            response.put("error", "invalid numBlocks or height");
            return response;
        }
        for (Block block : blocks) {
            blocksJSON.add(JSONData.block(block, true));
        }
        response.put("blocks", blocksJSON);
        return response;
    }

    @Override
    final boolean requirePost() {
        return true;
    }

}
