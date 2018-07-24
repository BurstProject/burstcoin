package brs.http;

import static brs.http.common.Parameters.FIRST_INDEX_PARAMETER;
import static brs.http.common.Parameters.LAST_INDEX_PARAMETER;
import static brs.http.common.ResultFields.ASSET_IDS_RESPONSE;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import brs.Asset;
import brs.assetexchange.AssetExchange;
import brs.common.AbstractUnitTest;
import brs.common.QuickMocker;
import brs.common.QuickMocker.MockParam;
import brs.db.BurstIterator;
import javax.servlet.http.HttpServletRequest;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.junit.Before;
import org.junit.Test;

public class GetAssetIdsTest extends AbstractUnitTest {

  private GetAssetIds t;

  private AssetExchange mockAssetExchange;

  @Before
  public void setUp() {
    mockAssetExchange = mock(AssetExchange.class);

    t = new GetAssetIds(mockAssetExchange);
  }

  @Test
  public void processRequest() {
    int firstIndex = 1;
    int lastIndex = 2;

    final Asset mockAsset = mock(Asset.class);
    when(mockAsset.getId()).thenReturn(5L);

    final BurstIterator<Asset> mockAssetIterator = mockBurstIterator(mockAsset);

    when(mockAssetExchange.getAllAssets(eq(firstIndex), eq(lastIndex)))
        .thenReturn(mockAssetIterator);

    final HttpServletRequest req = QuickMocker.httpServletRequest(
        new MockParam(FIRST_INDEX_PARAMETER, firstIndex),
        new MockParam(LAST_INDEX_PARAMETER, lastIndex)
    );

    final JSONObject result = (JSONObject) t.processRequest(req);

    assertNotNull(result);

    final JSONArray resultAssetIds = (JSONArray) result.get(ASSET_IDS_RESPONSE);
    assertNotNull(resultAssetIds);
    assertEquals(1, resultAssetIds.size());

    final String resultAssetId = (String) resultAssetIds.get(0);
    assertEquals("5", resultAssetId);
  }

}
