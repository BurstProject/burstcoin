package brs.http;

import static brs.http.common.Parameters.ACCOUNT_PARAMETER;
import static brs.http.common.Parameters.ASSET_PARAMETER;
import static brs.http.common.Parameters.FIRST_INDEX_PARAMETER;
import static brs.http.common.Parameters.LAST_INDEX_PARAMETER;
import static brs.http.common.ResultFields.BID_ORDER_IDS_RESPONSE;
import static org.junit.Assert.*;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import brs.Account;
import brs.BurstException;
import brs.Order.Bid;
import brs.assetexchange.AssetExchange;
import brs.common.AbstractUnitTest;
import brs.common.QuickMocker;
import brs.common.QuickMocker.MockParam;
import brs.db.BurstIterator;
import brs.services.ParameterService;
import javax.servlet.http.HttpServletRequest;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.junit.Before;
import org.junit.Test;

public class GetAccountCurrentBidOrderIdsTest extends AbstractUnitTest {

  private GetAccountCurrentBidOrderIds t;

  private ParameterService mockParameterService;
  private AssetExchange mockAssetExchange;

  @Before
  public void setUp() {
    mockParameterService = mock(ParameterService.class);
    mockAssetExchange = mock(AssetExchange.class);

    t = new GetAccountCurrentBidOrderIds(mockParameterService, mockAssetExchange);
  }

  @Test
  public void processRequest_byAccount() throws BurstException {
    final long accountId = 123L;
    final int firstIndex = 0;
    final int lastIndex = 1;

    final HttpServletRequest req = QuickMocker.httpServletRequest(
        new MockParam(ACCOUNT_PARAMETER, accountId),
        new MockParam(FIRST_INDEX_PARAMETER, firstIndex),
        new MockParam(LAST_INDEX_PARAMETER, lastIndex)
    );

    final Account mockAccount = mock(Account.class);
    when(mockAccount.getId()).thenReturn(accountId);

    final long mockBidId = 456L;
    final Bid bid = mock(Bid.class);
    when(bid.getId()).thenReturn(mockBidId);

    final BurstIterator<Bid> mockBidIterator = mockBurstIterator(bid);

    when(mockParameterService.getAccount(eq(req))).thenReturn(mockAccount);
    when(mockAssetExchange.getBidOrdersByAccount(eq(accountId), eq(firstIndex), eq(lastIndex))).thenReturn(mockBidIterator);

    final JSONObject result = (JSONObject) t.processRequest(req);
    assertNotNull(result);

    final JSONArray resultList = (JSONArray) result.get(BID_ORDER_IDS_RESPONSE);
    assertNotNull(resultList);
    assertEquals(1, resultList.size());
    assertEquals("" + mockBidId, resultList.get(0));
  }

  @Test
  public void processRequest_byAccountAsset() throws BurstException {
    final long accountId = 123L;
    final long assetId = 234L;
    final int firstIndex = 0;
    final int lastIndex = 1;

    final HttpServletRequest req = QuickMocker.httpServletRequest(
        new MockParam(ACCOUNT_PARAMETER, accountId),
        new MockParam(ASSET_PARAMETER, assetId),
        new MockParam(FIRST_INDEX_PARAMETER, firstIndex),
        new MockParam(LAST_INDEX_PARAMETER, lastIndex)
    );

    final Account mockAccount = mock(Account.class);
    when(mockAccount.getId()).thenReturn(accountId);

    final long mockBidId = 456L;
    final Bid bid = mock(Bid.class);
    when(bid.getId()).thenReturn(mockBidId);

    final BurstIterator<Bid> mockBidIterator = mockBurstIterator(bid);

    when(mockParameterService.getAccount(eq(req))).thenReturn(mockAccount);
    when(mockAssetExchange.getBidOrdersByAccountAsset(eq(accountId), eq(assetId), eq(firstIndex), eq(lastIndex))).thenReturn(mockBidIterator);

    final JSONObject result = (JSONObject) t.processRequest(req);
    assertNotNull(result);

    final JSONArray resultList = (JSONArray) result.get(BID_ORDER_IDS_RESPONSE);
    assertNotNull(resultList);
    assertEquals(1, resultList.size());
    assertEquals("" + mockBidId, resultList.get(0));
  }

}
