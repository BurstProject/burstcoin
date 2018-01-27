package brs.http;

import static brs.http.common.Parameters.ACCOUNT_PARAMETER;
import static brs.http.common.Parameters.ASSET_PARAMETER;
import static brs.http.common.Parameters.FIRST_INDEX_PARAMETER;
import static brs.http.common.Parameters.INCLUDE_ASSET_INFO_PARAMETER;
import static brs.http.common.Parameters.LAST_INDEX_PARAMETER;
import static brs.http.common.ResultFields.TRADES_RESPONSE;

import brs.Account;
import brs.Asset;
import brs.BurstException;
import brs.Trade;
import brs.db.BurstIterator;
import brs.db.sql.DbUtils;
import brs.http.common.Parameters;
import brs.services.AssetService;
import brs.services.ParameterService;
import brs.services.TradeService;
import brs.services.impl.TradeServiceImpl;
import brs.util.Convert;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

import javax.servlet.http.HttpServletRequest;

public final class GetTrades extends APIServlet.APIRequestHandler {

  private final ParameterService parameterService;
  private final AssetService assetService;
  private final TradeService tradeService;

  GetTrades(ParameterService parameterService, AssetService assetService, TradeService tradeService) {
    super(new APITag[] {APITag.AE}, ASSET_PARAMETER, ACCOUNT_PARAMETER, FIRST_INDEX_PARAMETER, LAST_INDEX_PARAMETER, INCLUDE_ASSET_INFO_PARAMETER);
    this.parameterService = parameterService;
    this.assetService = assetService;
    this.tradeService = tradeService;
  }

  @Override
  JSONStreamAware processRequest(HttpServletRequest req) throws BurstException {

    String assetId = Convert.emptyToNull(req.getParameter(ASSET_PARAMETER));
    String accountId = Convert.emptyToNull(req.getParameter(ACCOUNT_PARAMETER));

    int firstIndex = ParameterParser.getFirstIndex(req);
    int lastIndex = ParameterParser.getLastIndex(req);
    boolean includeAssetInfo = !Parameters.isFalse(req.getParameter(INCLUDE_ASSET_INFO_PARAMETER));

    JSONObject response = new JSONObject();
    JSONArray tradesData = new JSONArray();
    BurstIterator<Trade> trades = null;
    try {
      if (accountId == null) {
        Asset asset = parameterService.getAsset(req);
        trades = assetService.getTrades(asset.getId(), firstIndex, lastIndex);
      } else if (assetId == null) {
        Account account = parameterService.getAccount(req);
        trades = tradeService.getAccountTrades(account.getId(), firstIndex, lastIndex);
      } else {
        Asset asset = parameterService.getAsset(req);
        Account account = parameterService.getAccount(req);
        trades = tradeService.getAccountAssetTrades(account.getId(), asset.getId(), firstIndex, lastIndex);
      }
      while (trades.hasNext()) {
        final Trade trade = trades.next();
        final Asset asset = includeAssetInfo ? assetService.getAsset(trade.getAssetId()) : null;

        tradesData.add(JSONData.trade(trade, asset));
      }
    } finally {
      DbUtils.close(trades);
    }
    response.put(TRADES_RESPONSE, tradesData);

    return response;
  }

  @Override
  boolean startDbTransaction() {
    return true;
  }

}
