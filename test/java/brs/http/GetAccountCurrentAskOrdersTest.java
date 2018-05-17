package brs.http;

import static brs.http.common.Parameters.ACCOUNT_PARAMETER;
import static brs.http.common.Parameters.ASSET_PARAMETER;
import static brs.http.common.Parameters.FIRST_INDEX_PARAMETER;
import static brs.http.common.Parameters.LAST_INDEX_PARAMETER;
import static brs.http.common.ResultFields.ASK_ORDERS_RESPONSE;
import static junit.framework.TestCase.assertEquals;
import static junit.framework.TestCase.assertNotNull;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import brs.Account;
import brs.BurstException;
import brs.Order.Ask;
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

public class GetAccountCurrentAskOrdersTest extends AbstractUnitTest {

  private GetAccountCurrentAskOrders t;

  private ParameterService mockParameterService;
  private AssetExchange mockAssetExchange;

  @Before
  public void setUp() {
    mockParameterService = mock(ParameterService.class);
    mockAssetExchange = mock(AssetExchange.class);

    t = new GetAccountCurrentAskOrders(mockParameterService, mockAssetExchange);
  }

  @Test
  public void processRequest_getAskOrdersByAccount() throws BurstException {
    final long accountId = 2L;
    final int firstIndex = 1;
    final int lastIndex = 2;

    final HttpServletRequest req = QuickMocker.httpServletRequest(
        new MockParam(ACCOUNT_PARAMETER, accountId),
        new MockParam(FIRST_INDEX_PARAMETER, firstIndex),
        new MockParam(LAST_INDEX_PARAMETER, lastIndex)
    );

    final Account mockAccount = mock(Account.class);
    when(mockAccount.getId()).thenReturn(accountId);
    when(mockParameterService.getAccount(eq(req))).thenReturn(mockAccount);

    final Ask mockAsk = mock(Ask.class);
    final long mockAskId = 1L;
    when(mockAsk.getId()).thenReturn(mockAskId);

    final BurstIterator<Ask> mockAskIterator = mockBurstIterator(mockAsk);

    when(mockAssetExchange.getAskOrdersByAccount(eq(accountId), eq(firstIndex), eq(lastIndex))).thenReturn(mockAskIterator);

    final JSONObject result = (JSONObject) t.processRequest(req);

    assertNotNull(result);
    assertEquals(1, ((JSONArray) result.get(ASK_ORDERS_RESPONSE)).size());
  }

  @Test
  public void processRequest_getAskOrdersByAccountAsset() throws BurstException {
    final long assetId = 1L;
    final long accountId = 2L;
    final int firstIndex = 1;
    final int lastIndex = 2;

    final HttpServletRequest req = QuickMocker.httpServletRequest(
        new MockParam(ACCOUNT_PARAMETER, accountId),
        new MockParam(ASSET_PARAMETER, assetId),
        new MockParam(FIRST_INDEX_PARAMETER, firstIndex),
        new MockParam(LAST_INDEX_PARAMETER, lastIndex)
    );

    final Account mockAccount = mock(Account.class);
    when(mockAccount.getId()).thenReturn(accountId);
    when(mockParameterService.getAccount(eq(req))).thenReturn(mockAccount);

    final Ask mockAsk = mock(Ask.class);
    final long mockAskId = 1L;
    when(mockAsk.getId()).thenReturn(mockAskId);

    final BurstIterator<Ask> mockAskIterator = mockBurstIterator(mockAsk);

    when(mockAssetExchange.getAskOrdersByAccountAsset(eq(accountId), eq(assetId), eq(firstIndex), eq(lastIndex))).thenReturn(mockAskIterator);

    final JSONObject result = (JSONObject) t.processRequest(req);

    assertNotNull(result);
    assertEquals(1, ((JSONArray) result.get(ASK_ORDERS_RESPONSE)).size());
  }

}
