package nxt.http;

import nxt.Order;
import nxt.db.NxtIterator;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

import javax.servlet.http.HttpServletRequest;

public final class GetAllOpenBidOrders extends APIServlet.APIRequestHandler {

    static final GetAllOpenBidOrders instance = new GetAllOpenBidOrders();

    private GetAllOpenBidOrders() {
        super(new APITag[] {APITag.AE}, "firstIndex", "lastIndex");
    }

    @Override
    JSONStreamAware processRequest(HttpServletRequest req) {

        JSONObject response = new JSONObject();
        JSONArray ordersData = new JSONArray();

        int firstIndex = ParameterParser.getFirstIndex(req);
        int lastIndex = ParameterParser.getLastIndex(req);

        try (NxtIterator<Order.Bid> bidOrders = Order.Bid.getAll(firstIndex, lastIndex)) {
            while (bidOrders.hasNext()) {
                ordersData.add(JSONData.bidOrder(bidOrders.next()));
            }
        }

        response.put("openOrders", ordersData);
        return response;
    }

}
