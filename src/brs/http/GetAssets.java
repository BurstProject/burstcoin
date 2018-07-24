package brs.http;

import static brs.http.JSONResponses.INCORRECT_ASSET;
import static brs.http.JSONResponses.UNKNOWN_ASSET;
import static brs.http.common.Parameters.ASSETS_PARAMETER;
import static brs.http.common.ResultFields.ASSETS_RESPONSE;

import brs.Asset;
import brs.assetexchange.AssetExchange;
import brs.util.Convert;
import javax.servlet.http.HttpServletRequest;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

public final class GetAssets extends APIServlet.APIRequestHandler {

  private final AssetExchange assetExchange;

  public GetAssets(AssetExchange assetExchange) {
    super(new APITag[]{APITag.AE}, ASSETS_PARAMETER, ASSETS_PARAMETER, ASSETS_PARAMETER); // limit to 3 for testing
    this.assetExchange = assetExchange;
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
        Asset asset = assetExchange.getAsset(Convert.parseUnsignedLong(assetIdString));
        if (asset == null) {
          return UNKNOWN_ASSET;
        }

        int tradeCount = assetExchange.getTradeCount(asset.getId());
        int transferCount = assetExchange.getTransferCount(asset.getId());
        int accountsCount = assetExchange.getAssetAccountsCount(asset.getId());

        assetsJSONArray.add(JSONData.asset(asset, tradeCount, transferCount, accountsCount));
      } catch (RuntimeException e) {
        return INCORRECT_ASSET;
      }
    }
    return response;
  }

}
