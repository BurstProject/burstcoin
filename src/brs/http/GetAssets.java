package brs.http;

import static brs.http.JSONResponses.INCORRECT_ASSET;
import static brs.http.JSONResponses.UNKNOWN_ASSET;
import static brs.http.common.Parameters.ASSETS_PARAMETER;
import static brs.http.common.ResultFields.ASSETS_RESPONSE;

import brs.Asset;
import brs.services.AssetAccountService;
import brs.services.AssetService;
import brs.services.AssetTransferService;
import brs.services.TradeService;
import brs.util.Convert;
import javax.servlet.http.HttpServletRequest;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

public final class GetAssets extends APIServlet.APIRequestHandler {

  private final AssetService assetService;
  private final AssetAccountService assetAccountService;
  private final AssetTransferService assetTransferService;
  private final TradeService tradeService;

  public GetAssets(AssetService assetService, AssetAccountService assetAccountService, AssetTransferService assetTransferService, TradeService tradeService) {
    super(new APITag[]{APITag.AE}, ASSETS_PARAMETER, ASSETS_PARAMETER, ASSETS_PARAMETER); // limit to 3 for testing
    this.assetService = assetService;
    this.assetAccountService = assetAccountService;
    this.assetTransferService = assetTransferService;
    this.tradeService = tradeService;
  }

  @Override
  JSONStreamAware processRequest(HttpServletRequest req) {

    String[] assets = req.getParameterValues(ASSETS_PARAMETER);

    JSONObject response = new JSONObject();
    JSONArray assetsJSONArray = new JSONArray();
    response.put(ASSETS_RESPONSE, assetsJSONArray);
    for (String assetIdString : assets) {
      if (assetIdString == null || assetIdString.isEmpty()) {
        continue;
      }
      try {
        Asset asset = assetService.getAsset(Convert.parseUnsignedLong(assetIdString));
        if (asset == null) {
          return UNKNOWN_ASSET;
        }

        int tradeCount = tradeService.getTradeCount(asset.getId());
        int transferCount = assetTransferService.getTransferCount(asset.getId());
        int accountsCount = assetAccountService.getAssetAccountsCount(asset.getId());

        assetsJSONArray.add(JSONData.asset(asset, tradeCount, transferCount, accountsCount));
      } catch (RuntimeException e) {
        return INCORRECT_ASSET;
      }
    }
    return response;
  }

}
