package brs.http;

import static brs.http.JSONResponses.MISSING_SELLER;
import static brs.http.common.Parameters.FIRST_INDEX_PARAMETER;
import static brs.http.common.Parameters.LAST_INDEX_PARAMETER;
import static brs.http.common.Parameters.SELLER_PARAMETER;
import static brs.http.common.ResultFields.PURCHASES_RESPONSE;
import static org.junit.Assert.*;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import brs.BurstException;
import brs.DigitalGoodsStore.Purchase;
import brs.common.AbstractUnitTest;
import brs.common.QuickMocker;
import brs.common.QuickMocker.MockParam;
import brs.db.BurstIterator;
import brs.services.DGSGoodsStoreService;
import javax.servlet.http.HttpServletRequest;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.junit.Before;
import org.junit.Test;

public class GetDGSPendingPurchasesTest extends AbstractUnitTest {

  private GetDGSPendingPurchases t;

  private DGSGoodsStoreService mockDGSGoodStoreService;

  @Before
  public void setUp() {
    mockDGSGoodStoreService = mock(DGSGoodsStoreService.class);

    t = new GetDGSPendingPurchases(mockDGSGoodStoreService);
  }

  @Test
  public void processRequest() throws BurstException {
    final long sellerId = 123L;
    final int firstIndex = 1;
    final int lastIndex = 2;

    final HttpServletRequest req = QuickMocker.httpServletRequest(
        new MockParam(SELLER_PARAMETER, sellerId),
        new MockParam(FIRST_INDEX_PARAMETER, firstIndex),
        new MockParam(LAST_INDEX_PARAMETER, lastIndex)
    );

    final Purchase mockPurchase = mock(Purchase.class);

    final BurstIterator<Purchase> mockPurchaseIterator = mockBurstIterator(mockPurchase);
    when(mockDGSGoodStoreService.getPendingSellerPurchases(eq(sellerId), eq(firstIndex), eq(lastIndex))).thenReturn(mockPurchaseIterator);

    final JSONObject result = (JSONObject) t.processRequest(req);
    assertNotNull(result);

    final JSONArray resultPurchases = (JSONArray) result.get(PURCHASES_RESPONSE);

    assertNotNull(resultPurchases);
    assertEquals(1, resultPurchases.size());
  }

  @Test
  public void processRequest_missingSeller() throws BurstException {
    final HttpServletRequest req = QuickMocker.httpServletRequest(
        new MockParam(SELLER_PARAMETER, 0)
    );

    assertEquals(MISSING_SELLER, t.processRequest(req));
  }

}
