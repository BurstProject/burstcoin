package brs.http;

import static brs.http.common.Parameters.FIRST_INDEX_PARAMETER;
import static brs.http.common.Parameters.LAST_INDEX_PARAMETER;
import static brs.http.common.ResultFields.ASSET_IDS_RESPONSE;

import brs.Asset;
import brs.assetexchange.AssetExchange;
import brs.db.BurstIterator;
import brs.util.Convert;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

import javax.servlet.http.HttpServletRequest;

public final class GetAssetIds extends APIServlet.APIRequestHandler {

  private final AssetExchange assetExchange;

  public GetAssetIds(AssetExchange assetExchange) {
    super(new APITag[] {APITag.AE}, FIRST_INDEX_PARAMETER, LAST_INDEX_PARAMETER);
    this.assetExchange = assetExchange;
  }

  @Override
  JSONStreamAware processRequest(HttpServletRequest req) {

    int firstIndex = ParameterParser.getFirstIndex(req);
    int lastIndex = ParameterParser.getLastIndex(req);

    JSONArray assetIds = new JSONArray();
    try (BurstIterator<Asset> assets = assetExchange.getAllAssets(firstIndex, lastIndex)) {
      while (assets.hasNext()) {
        assetIds.add(Convert.toUnsignedLong(assets.next().getId()));
      }
    }
    JSONObject response = new JSONObject();
    response.put(ASSET_IDS_RESPONSE, assetIds);
    return response;
  }

}
