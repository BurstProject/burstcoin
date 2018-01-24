package brs.http;

import static brs.http.JSONResponses.UNKNOWN_GOODS;
import static brs.http.common.Parameters.PRICE_NQT_PARAMETER;
import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import brs.Account;
import brs.Blockchain;
import brs.BurstException;
import brs.DigitalGoodsStore.Goods;
import brs.TransactionProcessor;
import brs.common.QuickMocker;
import brs.common.QuickMocker.MockParam;
import brs.services.AccountService;
import brs.services.ParameterService;
import javax.servlet.http.HttpServletRequest;
import org.junit.Before;
import org.junit.Test;

public class DGSPriceChangeTest extends AbstractTransactionTest {

  private DGSPriceChange t;

  private ParameterService mockParameterService;
  private Blockchain mockBlockchain;
  private TransactionProcessor mockTransactionProcessor;
  private AccountService mockAccountService;

  @Before
  public void setUp() {
    mockParameterService = mock(ParameterService.class);
    mockBlockchain = mock(Blockchain.class);
    mockTransactionProcessor = mock(TransactionProcessor.class);
    mockAccountService = mock(AccountService.class);

    t = new DGSPriceChange(mockParameterService, mockTransactionProcessor, mockBlockchain, mockAccountService);
  }

  @Test
  public void processRequest() throws BurstException {
    final HttpServletRequest req = QuickMocker.httpServletRequest(
        new MockParam(PRICE_NQT_PARAMETER, 123L)
    );

    final Account mockAccount = mock(Account.class);
    when(mockAccount.getId()).thenReturn(1L);

    final Goods mockGoods = mock(Goods.class);
    when(mockGoods.getSellerId()).thenReturn(1L);
    when(mockGoods.isDelisted()).thenReturn(false);

    when(mockParameterService.getSenderAccount(eq(req))).thenReturn(mockAccount);
    when(mockParameterService.getGoods(eq(req))).thenReturn(mockGoods);

    t.processRequest(req);
  }

  @Test
  public void processRequest_goodsDelistedUnknownGoods() throws BurstException {
    final HttpServletRequest req = QuickMocker.httpServletRequest(
        new MockParam(PRICE_NQT_PARAMETER, 123L)
    );

    final Account mockAccount = mock(Account.class);

    final Goods mockGoods = mock(Goods.class);
    when(mockGoods.isDelisted()).thenReturn(true);

    when(mockParameterService.getSenderAccount(eq(req))).thenReturn(mockAccount);
    when(mockParameterService.getGoods(eq(req))).thenReturn(mockGoods);

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

    when(mockParameterService.getSenderAccount(eq(req))).thenReturn(mockAccount);
    when(mockParameterService.getGoods(eq(req))).thenReturn(mockGoods);

    assertEquals(UNKNOWN_GOODS, t.processRequest(req));
  }

}
