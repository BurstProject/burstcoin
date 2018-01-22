package brs.http;

import brs.Asset;
import brs.db.BurstIterator;
import brs.services.AssetAccountService;
import brs.services.AssetTransferService;
import brs.services.TradeService;
import org.json.simple.JSONArray;

public abstract class AbstractAssetsRetrieval extends APIServlet.APIRequestHandler  {

  private final TradeService tradeService;
  private final AssetTransferService assetTransferService;
  private final AssetAccountService assetAccountService;

  public AbstractAssetsRetrieval(APITag[] apiTags, TradeService tradeService, AssetTransferService assetTransferService, AssetAccountService assetAccountService, String... parameters) {
    super(apiTags, parameters);
    this.tradeService = tradeService;
    this.assetTransferService = assetTransferService;
    this.assetAccountService = assetAccountService;
  }

  protected JSONArray assetsToJson(BurstIterator<Asset> assets) {
    final JSONArray assetsJSONArray = new JSONArray();

    while (assets.hasNext()) {
      final Asset asset = assets.next();

      int tradeCount = tradeService.getTradeCount(asset.getId());
      int transferCount = assetTransferService.getTransferCount(asset.getId());
      int accountsCount = assetAccountService.getAssetAccountsCount(asset.getId());

      assetsJSONArray.add(JSONData.asset(asset, tradeCount, transferCount, accountsCount));
    }

    return assetsJSONArray;
  }
}
