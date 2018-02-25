package brs.http;

import static brs.TransactionType.DigitalGoods.PRICE_CHANGE;
import static brs.http.JSONResponses.UNKNOWN_GOODS;
import static brs.http.common.Parameters.PRICE_NQT_PARAMETER;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import brs.Account;
import brs.Attachment;
import brs.Blockchain;
import brs.BurstException;
import brs.DigitalGoodsStore.Goods;
import brs.common.QuickMocker;
import brs.common.QuickMocker.MockParam;
import brs.services.ParameterService;
import javax.servlet.http.HttpServletRequest;
import org.junit.Before;
import org.junit.Test;

public class DGSPriceChangeTest extends AbstractTransactionTest {

  private DGSPriceChange t;

  private ParameterService parameterServiceMock;
  private Blockchain blockchainMock;
  private APITransactionManager apiTransactionManagerMock;

  @Before
  public void setUp() {
    parameterServiceMock = mock(ParameterService.class);
    blockchainMock = mock(Blockchain.class);
    apiTransactionManagerMock = mock(APITransactionManager.class);

    t = new DGSPriceChange(parameterServiceMock, blockchainMock, apiTransactionManagerMock);
  }

  @Test
  public void processRequest() throws BurstException {
    final int priceNQTParameter = 5;

    final HttpServletRequest req = QuickMocker.httpServletRequest(
        new MockParam(PRICE_NQT_PARAMETER, priceNQTParameter)
    );

    final Account mockAccount = mock(Account.class);
    when(mockAccount.getId()).thenReturn(1L);

    long mockGoodsId = 123;
    final Goods mockGoods = mock(Goods.class);
    when(mockGoods.getId()).thenReturn(mockGoodsId);
    when(mockGoods.getSellerId()).thenReturn(1L);
    when(mockGoods.isDelisted()).thenReturn(false);

    when(parameterServiceMock.getSenderAccount(eq(req))).thenReturn(mockAccount);
    when(parameterServiceMock.getGoods(eq(req))).thenReturn(mockGoods);

    final Attachment.DigitalGoodsPriceChange attachment = (Attachment.DigitalGoodsPriceChange) attachmentCreatedTransaction(() -> t.processRequest(req), apiTransactionManagerMock);
    assertNotNull(attachment);

    assertEquals(PRICE_CHANGE, attachment.getTransactionType());
    assertEquals(mockGoodsId, attachment.getGoodsId());
    assertEquals(priceNQTParameter, attachment.getPriceNQT());
  }

  @Test
  public void processRequest_goodsDelistedUnknownGoods() throws BurstException {
    final HttpServletRequest req = QuickMocker.httpServletRequest(
        new MockParam(PRICE_NQT_PARAMETER, 123L)
    );

    final Account mockAccount = mock(Account.class);

    final Goods mockGoods = mock(Goods.class);
    when(mockGoods.isDelisted()).thenReturn(true);

    when(parameterServiceMock.getSenderAccount(eq(req))).thenReturn(mockAccount);
    when(parameterServiceMock.getGoods(eq(req))).thenReturn(mockGoods);

    assertEquals(UNKNOWN_GOODS, t.processRequest(req));
  }

  @Test
  public void processRequest_goodsWrongSellerIdUnknownGoods() throws BurstException {
    final HttpServletRequest req = QuickMocker.httpServletRequest(
        new MockParam(PRICE_NQT_PARAMETER, 123L)
    );

    final Account mockAccount = mock(Account.class);
    when(mockAccount.getId()).thenReturn(1L);

    final Goods mockGoods = mock(Goods.class);
    when(mockGoods.getSellerId()).thenReturn(2L);
    when(mockGoods.isDelisted()).thenReturn(false);

    when(parameterServiceMock.getSenderAccount(eq(req))).thenReturn(mockAccount);
    when(parameterServiceMock.getGoods(eq(req))).thenReturn(mockGoods);

    assertEquals(UNKNOWN_GOODS, t.processRequest(req));
  }

}
