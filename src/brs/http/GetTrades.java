package brs.http;

import static brs.http.common.Parameters.ACCOUNT_PARAMETER;
import static brs.http.common.Parameters.ASSET_PARAMETER;
import static brs.http.common.Parameters.FIRST_INDEX_PARAMETER;
import static brs.http.common.Parameters.INCLUDE_ASSET_INFO_PARAMETER;
import static brs.http.common.Parameters.LAST_INDEX_PARAMETER;

import brs.Account;
import brs.Asset;
import brs.BurstException;
import brs.Trade;
import brs.db.BurstIterator;
import brs.db.sql.DbUtils;
import brs.http.common.Parameters;
import brs.services.ParameterService;
import brs.util.Convert;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

import javax.servlet.http.HttpServletRequest;

public final class GetTrades extends APIServlet.APIRequestHandler {

  private ParameterService parameterService;

  GetTrades(ParameterService parameterService) {
    super(new APITag[] {APITag.AE}, ASSET_PARAMETER, ACCOUNT_PARAMETER, FIRST_INDEX_PARAMETER, LAST_INDEX_PARAMETER, INCLUDE_ASSET_INFO_PARAMETER);
    this.parameterService = parameterService;
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
        trades = asset.getTrades(firstIndex, lastIndex);
      } else if (assetId == null) {
        Account account = parameterService.getAccount(req);
        trades = account.getTrades(firstIndex, lastIndex);
      } else {
        Asset asset = parameterService.getAsset(req);
        Account account = parameterService.getAccount(req);
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
