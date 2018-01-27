package brs.http;

import brs.Asset;
import brs.BurstException;
import brs.Trade;
import brs.http.common.Parameters;
import brs.services.AssetService;
import brs.services.TradeService;
import brs.util.FilteringIterator;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

import javax.servlet.http.HttpServletRequest;

import static brs.http.common.Parameters.*;
import static brs.http.common.ResultFields.TRADES_RESPONSE;

public final class GetAllTrades extends APIServlet.APIRequestHandler {

  private final TradeService tradeService;
  private final AssetService assetService;

  GetAllTrades(TradeService tradeService, AssetService assetService) {
    super(new APITag[] {APITag.AE}, TIMESTAMP_PARAMETER, FIRST_INDEX_PARAMETER, LAST_INDEX_PARAMETER, INCLUDE_ASSET_INFO_PARAMETER);
    this.tradeService = tradeService;
    this.assetService = assetService;
  }
    
  @Override
  JSONStreamAware processRequest(HttpServletRequest req) throws BurstException {
    final int timestamp = ParameterParser.getTimestamp(req);
    final int firstIndex = ParameterParser.getFirstIndex(req);
    final int lastIndex = ParameterParser.getLastIndex(req);
    final boolean includeAssetInfo = !Parameters.isFalse(req.getParameter(INCLUDE_ASSET_INFO_PARAMETER));

    final JSONObject response = new JSONObject();
    final JSONArray trades = new JSONArray();

    try (FilteringIterator<Trade> tradeIterator = new FilteringIterator<>(
      tradeService.getAllTrades(0, -1),
      trade -> trade.getTimestamp() >= timestamp, firstIndex, lastIndex)) {
      while (tradeIterator.hasNext()) {
        final Trade trade = tradeIterator.next();
        final Asset asset = includeAssetInfo ? assetService.getAsset(trade.getAssetId()) : null;

        trades.add(JSONData.trade(trade, asset));
      }
    }

    response.put(TRADES_RESPONSE, trades);
    return response;
  }

}
