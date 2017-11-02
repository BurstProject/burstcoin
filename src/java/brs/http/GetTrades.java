package brs.http;

import brs.Account;
import brs.Asset;
import brs.BurstException;
import brs.Trade;
import brs.db.BurstIterator;
import brs.db.sql.DbUtils;
import brs.util.Convert;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

import javax.servlet.http.HttpServletRequest;

public final class GetTrades extends APIServlet.APIRequestHandler {

    static final GetTrades instance = new GetTrades();

    private GetTrades() {
        super(new APITag[] {APITag.AE}, "asset", "account", "firstIndex", "lastIndex", "includeAssetInfo");
    }

    @Override
    JSONStreamAware processRequest(HttpServletRequest req) throws BurstException {

        String assetId = Convert.emptyToNull(req.getParameter("asset"));
        String accountId = Convert.emptyToNull(req.getParameter("account"));

        int firstIndex = ParameterParser.getFirstIndex(req);
        int lastIndex = ParameterParser.getLastIndex(req);
        boolean includeAssetInfo = !"false".equalsIgnoreCase(req.getParameter("includeAssetInfo"));

        JSONObject response = new JSONObject();
        JSONArray tradesData = new JSONArray();
        BurstIterator<Trade> trades = null;
        try {
            if (accountId == null) {
                Asset asset = ParameterParser.getAsset(req);
                trades = asset.getTrades(firstIndex, lastIndex);
            } else if (assetId == null) {
                Account account = ParameterParser.getAccount(req);
                trades = account.getTrades(firstIndex, lastIndex);
            } else {
                Asset asset = ParameterParser.getAsset(req);
                Account account = ParameterParser.getAccount(req);
                trades = Trade.getAccountAssetTrades(account.getId(), asset.getId(), firstIndex, lastIndex);
            }
            while (trades.hasNext()) {
                tradesData.add(JSONData.trade(trades.next(), includeAssetInfo));
            }
        } finally {
            DbUtils.close(trades);
        }
        response.put("trades", tradesData);

        return response;
    }

    @Override
    boolean startDbTransaction() {
        return true;
    }

}
