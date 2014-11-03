package nxt.http;

import nxt.NxtException;
import nxt.Trade;
import nxt.db.FilteringIterator;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

import javax.servlet.http.HttpServletRequest;

public final class GetAllTrades extends APIServlet.APIRequestHandler {

    static final GetAllTrades instance = new GetAllTrades();

    private GetAllTrades() {
        super(new APITag[] {APITag.AE}, "timestamp", "firstIndex", "lastIndex", "includeAssetInfo");
    }
    
    @Override
    JSONStreamAware processRequest(HttpServletRequest req) throws NxtException {
        final int timestamp = ParameterParser.getTimestamp(req);
        int firstIndex = ParameterParser.getFirstIndex(req);
        int lastIndex = ParameterParser.getLastIndex(req);
        boolean includeAssetInfo = !"false".equalsIgnoreCase(req.getParameter("includeAssetInfo"));

        JSONObject response = new JSONObject();
        JSONArray trades = new JSONArray();
        try (FilteringIterator<Trade> tradeIterator = new FilteringIterator<>(Trade.getAllTrades(0, -1),
                new FilteringIterator.Filter<Trade>() {
                    @Override
                    public boolean ok(Trade trade) {
                        return trade.getTimestamp() >= timestamp;
                    }
                }, firstIndex, lastIndex)) {
            while (tradeIterator.hasNext()) {
                trades.add(JSONData.trade(tradeIterator.next(), includeAssetInfo));
            }
        }
        response.put("trades", trades);
        return response;
    }

}
