package brs.http;

import static brs.http.JSONResponses.GOODS_NOT_DELIVERED;
import static brs.http.JSONResponses.INCORRECT_PURCHASE;
import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import brs.Account;
import brs.Blockchain;
import brs.BurstException;
import brs.DigitalGoodsStore.Purchase;
import brs.TransactionProcessor;
import brs.common.QuickMocker;
import brs.crypto.EncryptedData;
import brs.services.AccountService;
import brs.services.ParameterService;
import brs.services.TransactionService;
import javax.servlet.http.HttpServletRequest;
import org.junit.Before;
import org.junit.Test;

public class DGSFeedbackTest extends AbstractTransactionTest {

  private DGSFeedback t;

  private ParameterService mockParameterService;
  private AccountService mockAccountService;
  private Blockchain blockchain;
  private TransactionProcessor mockTransactionProcessor;
  private TransactionService transactionServiceMock;

  @Before
  public void setUp() {
    mockParameterService = mock(ParameterService.class);
    mockAccountService = mock(AccountService.class);
    blockchain = mock(Blockchain.class);
    mockTransactionProcessor = mock(TransactionProcessor.class);
    transactionServiceMock = mock(TransactionService.class);

    t = new DGSFeedback(mockParameterService, mockTransactionProcessor, blockchain, mockAccountService, transactionServiceMock);
  }

  @Test
  public void processRequest() throws BurstException {
    final HttpServletRequest req = QuickMocker.httpServletRequest();

    final Purchase mockPurchase = mock(Purchase.class);
    final Account mockAccount = mock(Account.class);
    final Account mockSellerAccount = mock(Account.class);
    final EncryptedData mockEncryptedGoods = mock(EncryptedData.class);

    when(mockParameterService.getPurchase(eq(req))).thenReturn(mockPurchase);
    when(mockParameterService.getSenderAccount(eq(req))).thenReturn(mockAccount);
    when(mockAccountService.getAccount(eq(2L))).thenReturn(mockSellerAccount);

    when(mockAccount.getId()).thenReturn(1L);
    when(mockPurchase.getBuyerId()).thenReturn(1L);
    when(mockPurchase.getEncryptedGoods()).thenReturn(mockEncryptedGoods);
    when(mockPurchase.getSellerId()).thenReturn(2L);

    super.prepareTransactionTest(req, mockParameterService, mockTransactionProcessor, mockAccount);

    t.processRequest(req);
  }

  @Test
  public void processRequest_incorrectPurchaseWhenOtherBuyerId() throws BurstException {
    final HttpServletRequest req = QuickMocker.httpServletRequest();

    final Purchase mockPurchase = mock(Purchase.class);
    final Account mockAccount = mock(Account.class);

    when(mockParameterService.getPurchase(eq(req))).thenReturn(mockPurchase);
    when(mockParameterService.getSenderAccount(eq(req))).thenReturn(mockAccount);

    when(mockAccount.getId()).thenReturn(1L);
    when(mockPurchase.getBuyerId()).thenReturn(2L);

    assertEquals(INCORRECT_PURCHASE, t.processRequest(req));
  }

  @Test
  public void processRequest_goodsNotDeliveredWhenNoEncryptedGoods() throws BurstException {
    final HttpServletRequest req = QuickMocker.httpServletRequest();

    final Purchase mockPurchase = mock(Purchase.class);
    final Account mockAccount = mock(Account.class);

    when(mockParameterService.getPurchase(eq(req))).thenReturn(mockPurchase);
    when(mockParameterService.getSenderAccount(eq(req))).thenReturn(mockAccount);

    when(mockAccount.getId()).thenReturn(1L);
    when(mockPurchase.getBuyerId()).thenReturn(1L);
    when(mockPurchase.getEncryptedGoods()).thenReturn(null);

    assertEquals(GOODS_NOT_DELIVERED, t.processRequest(req));
  }

}
