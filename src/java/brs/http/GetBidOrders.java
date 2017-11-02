package brs.http;

import brs.BurstException;
import brs.Order;
import brs.db.BurstIterator;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

import javax.servlet.http.HttpServletRequest;

public final class GetBidOrders extends APIServlet.APIRequestHandler {

    static final GetBidOrders instance = new GetBidOrders();

    private GetBidOrders() {
        super(new APITag[] {APITag.AE}, "asset", "firstIndex", "lastIndex");
    }

    @Override
    JSONStreamAware processRequest(HttpServletRequest req) throws BurstException {

        long assetId = ParameterParser.getAsset(req).getId();
        int firstIndex = ParameterParser.getFirstIndex(req);
        int lastIndex = ParameterParser.getLastIndex(req);

        JSONArray orders = new JSONArray();
        try (BurstIterator<Order.Bid> bidOrders = Order.Bid.getSortedOrders(assetId, firstIndex, lastIndex)) {
            while (bidOrders.hasNext()) {
                orders.add(JSONData.bidOrder(bidOrders.next()));
            }
        }
        JSONObject response = new JSONObject();
        response.put("bidOrders", orders);
        return response;
    }

}
