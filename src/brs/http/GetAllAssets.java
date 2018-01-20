package brs.http;

import static brs.http.common.Parameters.FIRST_INDEX_PARAMETER;
import static brs.http.common.Parameters.LAST_INDEX_PARAMETER;
import static brs.http.common.ResultFields.ASSETS_RESPONSE;

import brs.Asset;
import brs.db.BurstIterator;
import brs.services.AssetAccountService;
import brs.services.AssetService;
import brs.services.AssetTransferService;
import brs.services.TradeService;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

import javax.servlet.http.HttpServletRequest;

public final class GetAllAssets extends APIServlet.APIRequestHandler {

  private final AssetService assetService;
  private final AssetAccountService assetAccountService;
  private final AssetTransferService assetTransferService;
  private final TradeService tradeService;

  public GetAllAssets(AssetService assetService, AssetAccountService assetAccountService, AssetTransferService assetTransferService, TradeService tradeService) {
    super(new APITag[] {APITag.AE}, FIRST_INDEX_PARAMETER, LAST_INDEX_PARAMETER);
    this.assetService = assetService;
    this.assetAccountService = assetAccountService;
    this.assetTransferService = assetTransferService;
    this.tradeService = tradeService;
  }

  @Override
  JSONStreamAware processRequest(HttpServletRequest req) {

    int firstIndex = ParameterParser.getFirstIndex(req);
    int lastIndex = ParameterParser.getLastIndex(req);

    JSONObject response = new JSONObject();
    JSONArray assetsJSONArray = new JSONArray();
    response.put(ASSETS_RESPONSE, assetsJSONArray);
    try (BurstIterator<Asset> assets = assetService.getAllAssets(firstIndex, lastIndex)) {
      while (assets.hasNext()) {
        Asset asset = assets.next();

        int tradeCount = tradeService.getTradeCount(asset.getId());
        int transferCount = assetTransferService.getTransferCount(asset.getId());
        int accountsCount = assetAccountService.getAssetAccountsCount(asset.getId());

        assetsJSONArray.add(JSONData.asset(asset, tradeCount, transferCount, accountsCount));
      }
    }
    return response;
  }

}
