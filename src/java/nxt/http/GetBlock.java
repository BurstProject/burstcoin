package nxt.http;

import nxt.Block;
import nxt.Nxt;
import nxt.util.Convert;
import org.json.simple.JSONStreamAware;

import javax.servlet.http.HttpServletRequest;

import static nxt.http.JSONResponses.INCORRECT_BLOCK;
import static nxt.http.JSONResponses.INCORRECT_HEIGHT;
import static nxt.http.JSONResponses.MISSING_BLOCK;
import static nxt.http.JSONResponses.UNKNOWN_BLOCK;

public final class GetBlock extends APIServlet.APIRequestHandler {

    static final GetBlock instance = new GetBlock();

    private GetBlock() {
        super(new APITag[] {APITag.BLOCKS}, "block", "height", "includeTransactions");
    }

    @Override
    JSONStreamAware processRequest(HttpServletRequest req) {

        int height = -1;
        String block = Convert.emptyToNull(req.getParameter("block"));
        String heightValue = Convert.emptyToNull(req.getParameter("height"));
        if (block == null) {
            try {
                if (heightValue != null) {
                    height = Integer.parseInt(heightValue);
                    if (height < 0 || height > Nxt.getBlockchain().getHeight()) {
                        return INCORRECT_HEIGHT;
                    }
                } else {
                    return MISSING_BLOCK;
                }
            } catch (RuntimeException e) {
                return INCORRECT_HEIGHT;
            }
        }

        boolean includeTransactions = "true".equalsIgnoreCase(req.getParameter("includeTransactions"));

        Block blockData;
        try {
            if (block != null) {
                blockData = Nxt.getBlockchain().getBlock(Convert.parseUnsignedLong(block));
            } else {
                blockData = Nxt.getBlockchain().getBlockAtHeight(height);
            }
            if (blockData == null) {
                return UNKNOWN_BLOCK;
            }
        } catch (RuntimeException e) {
            return INCORRECT_BLOCK;
        }

        return JSONData.block(blockData, includeTransactions);

    }

}