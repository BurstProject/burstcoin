package brs.http;

import brs.Asset;
import brs.db.BurstIterator;
import brs.util.Convert;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

import javax.servlet.http.HttpServletRequest;

import static brs.http.common.Parameters.FIRST_INDEX_PARAMETER;
import static brs.http.common.Parameters.LAST_INDEX_PARAMETER;

public final class GetAssetIds extends APIServlet.APIRequestHandler {

  static final GetAssetIds instance = new GetAssetIds();

  private GetAssetIds() {
    super(new APITag[] {APITag.AE}, FIRST_INDEX_PARAMETER, LAST_INDEX_PARAMETER);
  }

  @Override
  JSONStreamAware processRequest(HttpServletRequest req) {

    int firstIndex = ParameterParser.getFirstIndex(req);
    int lastIndex = ParameterParser.getLastIndex(req);

    JSONArray assetIds = new JSONArray();
    try (BurstIterator<Asset> assets = Asset.getAllAssets(firstIndex, lastIndex)) {
      while (assets.hasNext()) {
        assetIds.add(Convert.toUnsignedLong(assets.next().getId()));
      }
    }
    JSONObject response = new JSONObject();
    response.put("assetIds", assetIds);
    return response;
  }

}
