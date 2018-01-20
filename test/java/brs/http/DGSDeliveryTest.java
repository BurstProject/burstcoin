package brs.http;

import static brs.Constants.MAX_BALANCE_NQT;
import static brs.common.TestConstants.TEST_SECRET_PHRASE;
import static brs.http.JSONResponses.ALREADY_DELIVERED;
import static brs.http.JSONResponses.INCORRECT_DGS_DISCOUNT;
import static brs.http.JSONResponses.INCORRECT_DGS_GOODS;
import static brs.http.JSONResponses.INCORRECT_PURCHASE;
import static brs.http.common.Parameters.DISCOUNT_NQT_PARAMETER;
import static brs.http.common.Parameters.GOODS_TO_ENCRYPT_PARAMETER;
import static brs.http.common.Parameters.SECRET_PHRASE_PARAMETER;
import static org.junit.Assert.*;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import brs.Account;
import brs.Blockchain;
import brs.BurstException;
import brs.DigitalGoodsStore.Purchase;
import brs.TransactionProcessor;
import brs.common.QuickMocker;
import brs.common.QuickMocker.MockParam;
import brs.services.AccountService;
import brs.services.AliasService;
import brs.services.ParameterService;
import javax.servlet.http.HttpServletRequest;
import org.junit.Before;
import org.junit.Test;

public class DGSDeliveryTest extends AbstractTransactionTest {

  private DGSDelivery t;

  private ParameterService mockParameterService;
  private TransactionProcessor mockTransactionProcessor;
  private Blockchain mockBlockchain;
  private AccountService mockAccountService;

  @Before
  public void setUp() {
    mockParameterService = mock(ParameterService.class);
    mockTransactionProcessor = mock(TransactionProcessor.class);
    mockBlockchain = mock(Blockchain.class);
    mockAccountService = mock(AccountService.class);

    t = new DGSDelivery(mockParameterService, mockTransactionProcessor, mockBlockchain, mockAccountService);
  }

  @Test
  public void processRequest() throws BurstException {
    final HttpServletRequest req = QuickMocker.httpServletRequest(
        new MockParam(DISCOUNT_NQT_PARAMETER, "9"),
        new MockParam(GOODS_TO_ENCRYPT_PARAMETER, "abc"),
        new MockParam(SECRET_PHRASE_PARAMETER, TEST_SECRET_PHRASE)
    );

    final Account mockSellerAccount = mock(Account.class);
    final Account mockBuyerAccount = mock(Account.class);
    final Purchase mockPurchase = mock(Purchase.class);

    when(mockSellerAccount.getId()).thenReturn(1l);
    when(mockPurchase.getSellerId()).thenReturn(1l);
    when(mockPurchase.getBuyerId()).thenReturn(2L);
    when(mockPurchase.getQuantity()).thenReturn(9);
    when(mockPurchase.getPriceNQT()).thenReturn(1L);

    when(mockPurchase.isPending()).thenReturn(true);

    when(mockParameterService.getSenderAccount(eq(req))).thenReturn(mockSellerAccount);
    when(mockParameterService.getPurchase(eq(req))).thenReturn(mockPurchase);
    when(mockAccountService.getAccount(eq(mockPurchase.getBuyerId()))).thenReturn(mockBuyerAccount);

    super.prepareTransactionTest(req, mockParameterService, mockTransactionProcessor, mock(AliasService.class), mockSellerAccount);

    t.processRequest(req);
  }

  @Test
  public void processRequest_sellerAccountIdDifferentFromAccountSellerIdIsIncorrectPurchase() throws BurstException {
    final HttpServletRequest req = QuickMocker.httpServletRequest();

    final Account mockSellerAccount = mock(Account.class);
    final Purchase mockPurchase = mock(Purchase.class);

    when(mockSellerAccount.getId()).thenReturn(1l);
    when(mockPurchase.getSellerId()).thenReturn(2l);

    when(mockParameterService.getSenderAccount(eq(req))).thenReturn(mockSellerAccount);
    when(mockParameterService.getPurchase(eq(req))).thenReturn(mockPurchase);

    assertEquals(INCORRECT_PURCHASE, t.processRequest(req));
  }

  @Test
  public void processRequest_purchaseNotPendingIsAlreadyDelivered() throws BurstException {
    final HttpServletRequest req = QuickMocker.httpServletRequest();

    final Account mockSellerAccount = mock(Account.class);
    final Purchase mockPurchase = mock(Purchase.class);

    when(mockSellerAccount.getId()).thenReturn(1l);
    when(mockPurchase.getSellerId()).thenReturn(1l);

    when(mockPurchase.isPending()).thenReturn(false);

    when(mockParameterService.getSenderAccount(eq(req))).thenReturn(mockSellerAccount);
    when(mockParameterService.getPurchase(eq(req))).thenReturn(mockPurchase);

    assertEquals(ALREADY_DELIVERED, t.processRequest(req));
  }

  @Test
  public void processRequest_dgsDiscountNotAValidNumberIsIncorrectDGSDiscount() throws BurstException {
    final HttpServletRequest req = QuickMocker.httpServletRequest(
        new MockParam(DISCOUNT_NQT_PARAMETER, "Bob")
    );

    final Account mockSellerAccount = mock(Account.class);
    final Purchase mockPurchase = mock(Purchase.class);

    when(mockSellerAccount.getId()).thenReturn(1l);
    when(mockPurchase.getSellerId()).thenReturn(1l);

    when(mockPurchase.isPending()).thenReturn(true);

    when(mockParameterService.getSenderAccount(eq(req))).thenReturn(mockSellerAccount);
    when(mockParameterService.getPurchase(eq(req))).thenReturn(mockPurchase);

    assertEquals(INCORRECT_DGS_DISCOUNT, t.processRequest(req));
  }

  @Test
  public void processRequest_dgsDiscountNegativeIsIncorrectDGSDiscount() throws BurstException {
    final HttpServletRequest req = QuickMocker.httpServletRequest(
        new MockParam(DISCOUNT_NQT_PARAMETER, "-1")
    );

    final Account mockSellerAccount = mock(Account.class);
    final Purchase mockPurchase = mock(Purchase.class);

    when(mockSellerAccount.getId()).thenReturn(1l);
    when(mockPurchase.getSellerId()).thenReturn(1l);

    when(mockPurchase.isPending()).thenReturn(true);

    when(mockParameterService.getSenderAccount(eq(req))).thenReturn(mockSellerAccount);
    when(mockParameterService.getPurchase(eq(req))).thenReturn(mockPurchase);

    assertEquals(INCORRECT_DGS_DISCOUNT, t.processRequest(req));
  }

  @Test
  public void processRequest_dgsDiscountOverMaxBalanceNQTIsIncorrectDGSDiscount() throws BurstException {
    final HttpServletRequest req = QuickMocker.httpServletRequest(
        new MockParam(DISCOUNT_NQT_PARAMETER, "" + (MAX_BALANCE_NQT + 1))
    );

    final Account mockSellerAccount = mock(Account.class);
    final Purchase mockPurchase = mock(Purchase.class);

    when(mockSellerAccount.getId()).thenReturn(1l);
    when(mockPurchase.getSellerId()).thenReturn(1l);

    when(mockPurchase.isPending()).thenReturn(true);

    when(mockParameterService.getSenderAccount(eq(req))).thenReturn(mockSellerAccount);
    when(mockParameterService.getPurchase(eq(req))).thenReturn(mockPurchase);

    assertEquals(INCORRECT_DGS_DISCOUNT, t.processRequest(req));
  }

  @Test
  public void processRequest_dgsDiscountNegativeIsNotSafeMultiply() throws BurstException {
    final HttpServletRequest req = QuickMocker.httpServletRequest(
        new MockParam(DISCOUNT_NQT_PARAMETER, "99999999999")
    );

    final Account mockSellerAccount = mock(Account.class);
    final Purchase mockPurchase = mock(Purchase.class);

    when(mockSellerAccount.getId()).thenReturn(1l);
    when(mockPurchase.getSellerId()).thenReturn(1l);
    when(mockPurchase.getQuantity()).thenReturn(999999999);
    when(mockPurchase.getPriceNQT()).thenReturn(1L);

    when(mockPurchase.isPending()).thenReturn(true);

    when(mockParameterService.getSenderAccount(eq(req))).thenReturn(mockSellerAccount);
    when(mockParameterService.getPurchase(eq(req))).thenReturn(mockPurchase);

    assertEquals(INCORRECT_DGS_DISCOUNT, t.processRequest(req));
  }

  @Test
  public void processRequest_goodsToEncryptIsEmptyIsIncorrectDGSGoods() throws BurstException {
      final HttpServletRequest req = QuickMocker.httpServletRequest(
          new MockParam(DISCOUNT_NQT_PARAMETER, "9"),
          new MockParam(GOODS_TO_ENCRYPT_PARAMETER, ""),
          new MockParam(SECRET_PHRASE_PARAMETER, TEST_SECRET_PHRASE)
      );

      final Account mockSellerAccount = mock(Account.class);
      final Purchase mockPurchase = mock(Purchase.class);

      when(mockSellerAccount.getId()).thenReturn(1l);
      when(mockPurchase.getSellerId()).thenReturn(1l);
      when(mockPurchase.getQuantity()).thenReturn(9);
    when(mockPurchase.getPriceNQT()).thenReturn(1L);

      when(mockPurchase.isPending()).thenReturn(true);

      when(mockParameterService.getSenderAccount(eq(req))).thenReturn(mockSellerAccount);
      when(mockParameterService.getPurchase(eq(req))).thenReturn(mockPurchase);

      assertEquals(INCORRECT_DGS_GOODS, t.processRequest(req));
  }

  @Test
  public void processRequest_goodsToEncryptIsNotHexStringIncorrectDGSGoods() throws BurstException {
    final HttpServletRequest req = QuickMocker.httpServletRequest(
        new MockParam(DISCOUNT_NQT_PARAMETER, "9"),
        new MockParam(GOODS_TO_ENCRYPT_PARAMETER, "ZZZ"),
        new MockParam(SECRET_PHRASE_PARAMETER, TEST_SECRET_PHRASE)
    );

    final Account mockSellerAccount = mock(Account.class);
    final Purchase mockPurchase = mock(Purchase.class);

    when(mockSellerAccount.getId()).thenReturn(1l);
    when(mockPurchase.getSellerId()).thenReturn(1l);
    when(mockPurchase.getQuantity()).thenReturn(9);
    when(mockPurchase.getPriceNQT()).thenReturn(1L);

    when(mockPurchase.isPending()).thenReturn(true);

    when(mockParameterService.getSenderAccount(eq(req))).thenReturn(mockSellerAccount);
    when(mockParameterService.getPurchase(eq(req))).thenReturn(mockPurchase);

    assertEquals(INCORRECT_DGS_GOODS, t.processRequest(req));
  }

}
