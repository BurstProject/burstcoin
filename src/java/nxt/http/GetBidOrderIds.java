package nxt.http;

import nxt.NxtException;
import nxt.Order;
import nxt.util.Convert;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

import javax.servlet.http.HttpServletRequest;
import java.util.Iterator;

public final class GetBidOrderIds extends APIServlet.APIRequestHandler {

    static final GetBidOrderIds instance = new GetBidOrderIds();

    private GetBidOrderIds() {
        super("asset", "limit");
    }

    @Override
    JSONStreamAware processRequest(HttpServletRequest req) throws NxtException {

        Long assetId = ParameterParser.getAsset(req).getId();

        int limit;
        try {
            limit = Integer.parseInt(req.getParameter("limit"));
        } catch (NumberFormatException e) {
            limit = Integer.MAX_VALUE;
        }

        JSONArray orderIds = new JSONArray();
        Iterator<Order.Bid> bidOrders = Order.Bid.getSortedOrders(assetId).iterator();
        while (bidOrders.hasNext() && limit-- > 0) {
            orderIds.add(Convert.toUnsignedLong(bidOrders.next().getId()));
        }

        JSONObject response = new JSONObject();
        response.put("bidOrderIds", orderIds);
        return response;
    }

}
