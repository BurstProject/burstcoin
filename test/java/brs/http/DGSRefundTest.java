package brs.http;

import static brs.http.JSONResponses.DUPLICATE_REFUND;
import static brs.http.JSONResponses.GOODS_NOT_DELIVERED;
import static brs.http.JSONResponses.INCORRECT_DGS_REFUND;
import static brs.http.JSONResponses.INCORRECT_PURCHASE;
import static brs.http.common.Parameters.REFUND_NQT_PARAMETER;
import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import brs.Account;
import brs.Blockchain;
import brs.BurstException;
import brs.Constants;
import brs.DigitalGoodsStore.Purchase;
import brs.TransactionProcessor;
import brs.common.QuickMocker;
import brs.common.QuickMocker.MockParam;
import brs.crypto.EncryptedData;
import brs.services.AccountService;
import brs.services.ParameterService;
import javax.servlet.http.HttpServletRequest;
import org.junit.Before;
import org.junit.Test;

public class DGSRefundTest extends AbstractTransactionTest {

  private DGSRefund t;

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

    t = new DGSRefund(mockParameterService, mockTransactionProcessor, mockBlockchain, mockAccountService);
  }

  @Test
  public void processRequest() throws BurstException {
    final HttpServletRequest req = QuickMocker.httpServletRequest(
        new MockParam(REFUND_NQT_PARAMETER, 5)
    );

    final Account mockSellerAccount = mock(Account.class);
    when(mockSellerAccount.getId()).thenReturn(1L);

    final Purchase mockPurchase = mock(Purchase.class);
    when(mockPurchase.getSellerId()).thenReturn(1L);
    when(mockPurchase.getBuyerId()).thenReturn(2L);
    when(mockPurchase.getRefundNote()).thenReturn(null);
    when(mockPurchase.getEncryptedGoods()).thenReturn(mock(EncryptedData.class));

    when(mockParameterService.getSenderAccount(eq(req))).thenReturn(mockSellerAccount);
    when(mockParameterService.getPurchase(eq(req))).thenReturn(mockPurchase);

    final Account mockBuyerAccount = mock(Account.class);

    when(mockAccountService.getAccount(eq(mockPurchase.getBuyerId()))).thenReturn(mockBuyerAccount);

    prepareTransactionTest(req, mockParameterService, mockTransactionProcessor, mockSellerAccount);

    t.processRequest(req);
  }

  @Test
  public void processRequest_incorrectPurchase() throws BurstException {
    final HttpServletRequest req = QuickMocker.httpServletRequest();

    final Account mockSellerAccount = mock(Account.class);
    when(mockSellerAccount.getId()).thenReturn(1L);

    final Purchase mockPurchase = mock(Purchase.class);
    when(mockPurchase.getSellerId()).thenReturn(2L);

    when(mockParameterService.getSenderAccount(eq(req))).thenReturn(mockSellerAccount);
    when(mockParameterService.getPurchase(eq(req))).thenReturn(mockPurchase);

    assertEquals(INCORRECT_PURCHASE, t.processRequest(req));
  }

  @Test
  public void processRequest_duplicateRefund() throws BurstException {
    final HttpServletRequest req = QuickMocker.httpServletRequest();

    final Account mockSellerAccount = mock(Account.class);
    when(mockSellerAccount.getId()).thenReturn(1L);

    final Purchase mockPurchase = mock(Purchase.class);
    when(mockPurchase.getSellerId()).thenReturn(1L);
    when(mockPurchase.getRefundNote()).thenReturn(mock(EncryptedData.class));

    when(mockParameterService.getSenderAccount(eq(req))).thenReturn(mockSellerAccount);
    when(mockParameterService.getPurchase(eq(req))).thenReturn(mockPurchase);

    assertEquals(DUPLICATE_REFUND, t.processRequest(req));
  }

  @Test
  public void processRequest_goodsNotDelivered() throws BurstException {
    final HttpServletRequest req = QuickMocker.httpServletRequest();

    final Account mockSellerAccount = mock(Account.class);
    when(mockSellerAccount.getId()).thenReturn(1L);

    final Purchase mockPurchase = mock(Purchase.class);
    when(mockPurchase.getSellerId()).thenReturn(1L);
    when(mockPurchase.getRefundNote()).thenReturn(null);
    when(mockPurchase.getEncryptedGoods()).thenReturn(null);

    when(mockParameterService.getSenderAccount(eq(req))).thenReturn(mockSellerAccount);
    when(mockParameterService.getPurchase(eq(req))).thenReturn(mockPurchase);

    assertEquals(GOODS_NOT_DELIVERED, t.processRequest(req));
  }

  @Test
  public void processRequest_incorrectDgsRefundWrongFormat() throws BurstException {
    final HttpServletRequest req = QuickMocker.httpServletRequest(
        new MockParam(REFUND_NQT_PARAMETER, "Bob")
    );

    final Account mockSellerAccount = mock(Account.class);
    when(mockSellerAccount.getId()).thenReturn(1L);

    final Purchase mockPurchase = mock(Purchase.class);
    when(mockPurchase.getSellerId()).thenReturn(1L);
    when(mockPurchase.getRefundNote()).thenReturn(null);
    when(mockPurchase.getEncryptedGoods()).thenReturn(mock(EncryptedData.class));

    when(mockParameterService.getSenderAccount(eq(req))).thenReturn(mockSellerAccount);
    when(mockParameterService.getPurchase(eq(req))).thenReturn(mockPurchase);

    assertEquals(INCORRECT_DGS_REFUND, t.processRequest(req));
  }

  @Test
  public void processRequest_negativeIncorrectDGSRefund() throws BurstException {
    final HttpServletRequest req = QuickMocker.httpServletRequest(
        new MockParam(REFUND_NQT_PARAMETER, -5)
    );

    final Account mockSellerAccount = mock(Account.class);
    when(mockSellerAccount.getId()).thenReturn(1L);

    final Purchase mockPurchase = mock(Purchase.class);
    when(mockPurchase.getSellerId()).thenReturn(1L);
    when(mockPurchase.getRefundNote()).thenReturn(null);
    when(mockPurchase.getEncryptedGoods()).thenReturn(mock(EncryptedData.class));

    when(mockParameterService.getSenderAccount(eq(req))).thenReturn(mockSellerAccount);
    when(mockParameterService.getPurchase(eq(req))).thenReturn(mockPurchase);

    assertEquals(INCORRECT_DGS_REFUND, t.processRequest(req));
  }

  @Test
  public void processRequest_overMaxBalanceNQTIncorrectDGSRefund() throws BurstException {
    final HttpServletRequest req = QuickMocker.httpServletRequest(
        new MockParam(REFUND_NQT_PARAMETER, Constants.MAX_BALANCE_NQT + 1)
    );

    final Account mockSellerAccount = mock(Account.class);
    when(mockSellerAccount.getId()).thenReturn(1L);

    final Purchase mockPurchase = mock(Purchase.class);
    when(mockPurchase.getSellerId()).thenReturn(1L);
    when(mockPurchase.getRefundNote()).thenReturn(null);
    when(mockPurchase.getEncryptedGoods()).thenReturn(mock(EncryptedData.class));

    when(mockParameterService.getSenderAccount(eq(req))).thenReturn(mockSellerAccount);
    when(mockParameterService.getPurchase(eq(req))).thenReturn(mockPurchase);

    assertEquals(INCORRECT_DGS_REFUND, t.processRequest(req));
  }

}
