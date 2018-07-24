package brs.http;

import brs.Asset;
import brs.assetexchange.AssetExchange;
import brs.db.BurstIterator;
import org.json.simple.JSONArray;

public abstract class AbstractAssetsRetrieval extends APIServlet.APIRequestHandler  {

  private final AssetExchange assetExchange;

  public AbstractAssetsRetrieval(APITag[] apiTags, AssetExchange assetExchange, String... parameters) {
    super(apiTags, parameters);
    this.assetExchange = assetExchange;
  }

  protected JSONArray assetsToJson(BurstIterator<Asset> assets) {
    final JSONArray assetsJSONArray = new JSONArray();

    while (assets.hasNext()) {
      final Asset asset = assets.next();

      int tradeCount = assetExchange.getTradeCount(asset.getId());
      int transferCount = assetExchange.getTransferCount(asset.getId());
      int accountsCount = assetExchange.getAssetAccountsCount(asset.getId());

      assetsJSONArray.add(JSONData.asset(asset, tradeCount, transferCount, accountsCount));
    }

    return assetsJSONArray;
  }
}
