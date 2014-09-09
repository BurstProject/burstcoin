package nxt.http;

import nxt.NxtException;
import nxt.Trade;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

import javax.servlet.http.HttpServletRequest;
import java.util.List;

public final class GetTrades extends APIServlet.APIRequestHandler {

    static final GetTrades instance = new GetTrades();

    private GetTrades() {
        super(new APITag[] {APITag.AE}, "asset", "firstIndex", "lastIndex");
    }

    @Override
    JSONStreamAware processRequest(HttpServletRequest req) throws NxtException {

        Long assetId = ParameterParser.getAsset(req).getId();
        int firstIndex = ParameterParser.getFirstIndex(req);
        int lastIndex = ParameterParser.getLastIndex(req);

        JSONObject response = new JSONObject();

        JSONArray tradesData = new JSONArray();
        try {
            List<Trade> trades = Trade.getTrades(assetId);
            for (int i = firstIndex; i <= lastIndex && i < trades.size(); i++) {
                tradesData.add(JSONData.trade(trades.get(trades.size() - 1 - i)));
            }
        } catch (RuntimeException e) {
            response.put("error", e.toString());
        }
        response.put("trades", tradesData);

        return response;
    }

}
