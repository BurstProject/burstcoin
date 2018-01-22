package brs.http;

import static brs.http.JSONResponses.UNKNOWN_GOODS;
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
import brs.services.AccountService;
import brs.services.ParameterService;
import javax.servlet.http.HttpServletRequest;
import org.junit.Before;
import org.junit.Test;

public class DGSDelistingTest extends AbstractTransactionTest {

  private DGSDelisting t;

  private ParameterService mockParameterService;
  private TransactionProcessor mockTransactionProcessor;
  private Blockchain mockBlockchain;
  private AccountService mockAccountService;

  @Before
  public void setUp() {
    mockParameterService = mock(ParameterService.class);
    mockBlockchain = mock(Blockchain.class);
    mockTransactionProcessor = mock(TransactionProcessor.class);
    mockAccountService = mock(AccountService.class);

    t = new DGSDelisting(mockParameterService, mockTransactionProcessor, mockBlockchain, mockAccountService);
  }

  @Test
  public void processRequest() throws BurstException {
    final HttpServletRequest req = QuickMocker.httpServletRequest();

    final Account mockAccount = mock(Account.class);
    final Goods mockGoods = mock(Goods.class);

    when(mockGoods.isDelisted()).thenReturn(false);
    when(mockGoods.getSellerId()).thenReturn(1L);
    when(mockAccount.getId()).thenReturn(1L);

    when(mockParameterService.getSenderAccount(eq(req))).thenReturn(mockAccount);
    when(mockParameterService.getGoods(eq(req))).thenReturn(mockGoods);

    super.prepareTransactionTest(req, mockParameterService, mockTransactionProcessor, mockAccount);

    t.processRequest(req);
  }

  @Test
  public void processRequest_goodsDelistedUnknownGoods() throws BurstException {
    final HttpServletRequest req = QuickMocker.httpServletRequest();

    final Account mockAccount = mock(Account.class);
    final Goods mockGoods = mock(Goods.class);

    when(mockGoods.isDelisted()).thenReturn(true);

    when(mockParameterService.getSenderAccount(eq(req))).thenReturn(mockAccount);
    when(mockParameterService.getGoods(eq(req))).thenReturn(mockGoods);

    assertEquals(UNKNOWN_GOODS, t.processRequest(req));
  }

  @Test
  public void processRequest_otherSellerIdUnknownGoods() throws BurstException {
    final HttpServletRequest req = QuickMocker.httpServletRequest();

    final Account mockAccount = mock(Account.class);
    final Goods mockGoods = mock(Goods.class);

    when(mockGoods.isDelisted()).thenReturn(false);
    when(mockGoods.getSellerId()).thenReturn(1L);
    when(mockAccount.getId()).thenReturn(2L);

    when(mockParameterService.getSenderAccount(eq(req))).thenReturn(mockAccount);
    when(mockParameterService.getGoods(eq(req))).thenReturn(mockGoods);

    assertEquals(UNKNOWN_GOODS, t.processRequest(req));
  }

}
