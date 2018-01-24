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
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

import javax.servlet.http.HttpServletRequest;

public final class GetAllAssets extends AbstractAssetsRetrieval {

  private final AssetService assetService;

  public GetAllAssets(AssetService assetService, AssetAccountService assetAccountService, AssetTransferService assetTransferService, TradeService tradeService) {
    super(new APITag[] {APITag.AE}, tradeService, assetTransferService, assetAccountService, FIRST_INDEX_PARAMETER, LAST_INDEX_PARAMETER);
    this.assetService = assetService;
  }

  @Override
  JSONStreamAware processRequest(HttpServletRequest req) {
    int firstIndex = ParameterParser.getFirstIndex(req);
    int lastIndex = ParameterParser.getLastIndex(req);

    JSONObject response = new JSONObject();

    try (BurstIterator<Asset> assets = assetService.getAllAssets(firstIndex, lastIndex)) {
      response.put(ASSETS_RESPONSE, assetsToJson(assets));
    }

    return response;
  }

}
