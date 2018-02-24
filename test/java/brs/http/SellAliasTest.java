package brs.http;

import static brs.Constants.MAX_BALANCE_NQT;
import static brs.http.JSONResponses.INCORRECT_ALIAS_OWNER;
import static brs.http.JSONResponses.INCORRECT_PRICE;
import static brs.http.JSONResponses.INCORRECT_RECIPIENT;
import static brs.http.JSONResponses.MISSING_PRICE;
import static brs.http.common.Parameters.PRICE_NQT_PARAMETER;
import static brs.http.common.Parameters.RECIPIENT_PARAMETER;
import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import brs.Account;
import brs.Alias;
import brs.Blockchain;
import brs.BurstException;
import brs.TransactionProcessor;
import brs.common.QuickMocker;
import brs.common.QuickMocker.MockParam;
import brs.services.AccountService;
import brs.services.ParameterService;
import brs.services.TransactionService;
import javax.servlet.http.HttpServletRequest;
import org.junit.Before;
import org.junit.Test;

public class SellAliasTest extends AbstractTransactionTest {

  private SellAlias t;

  private ParameterService mockParameterService;
  private Blockchain mockBlockchain;
  private TransactionProcessor mockTransactionProcessor;
  private AccountService mockAccountService;
  private TransactionService transactionServiceMock;

  @Before
  public void setUp() {
    mockParameterService = mock(ParameterService.class);
    mockBlockchain = mock(Blockchain.class);
    mockTransactionProcessor = mock(TransactionProcessor.class);
    mockAccountService = mock(AccountService.class);
    transactionServiceMock = mock(TransactionService.class);

    t = new SellAlias(mockParameterService, mockTransactionProcessor, mockBlockchain, mockAccountService, transactionServiceMock);
  }

  @Test
  public void processRequest() throws BurstException {
    final int price = 10;
    final int recipientId = 5;

    final HttpServletRequest req = QuickMocker.httpServletRequest(
        new MockParam(PRICE_NQT_PARAMETER, price),
        new MockParam(RECIPIENT_PARAMETER, recipientId)
    );

    final long aliasAccountId = 1L;
    final Alias mockAlias = mock(Alias.class);
    when(mockAlias.getAccountId()).thenReturn(aliasAccountId);

    final Account mockSender = mock(Account.class);
    when(mockSender.getId()).thenReturn(aliasAccountId);

    when(mockParameterService.getSenderAccount(req)).thenReturn(mockSender);
    when(mockParameterService.getAlias(req)).thenReturn(mockAlias);

    prepareTransactionTest(req, mockParameterService, mockTransactionProcessor, mockSender);

    t.processRequest(req);
  }

  @Test
  public void processRequest_missingPrice() throws BurstException {
    final HttpServletRequest req = QuickMocker.httpServletRequest();

    assertEquals(MISSING_PRICE, t.processRequest(req));
  }

  @Test
  public void processRequest_incorrectPrice_unParsable() throws BurstException {
    final HttpServletRequest req = QuickMocker.httpServletRequest(
      new MockParam(PRICE_NQT_PARAMETER, "unParsable")
    );

    assertEquals(INCORRECT_PRICE, t.processRequest(req));
  }

  @Test(expected = ParameterException.class)
  public void processRequest_incorrectPrice_negative() throws BurstException {
    final HttpServletRequest req = QuickMocker.httpServletRequest(
        new MockParam(PRICE_NQT_PARAMETER, -10)
    );

    t.processRequest(req);
  }

  @Test(expected = ParameterException.class)
  public void processRequest_incorrectPrice_overMaxBalance() throws BurstException {
    final HttpServletRequest req = QuickMocker.httpServletRequest(
        new MockParam(PRICE_NQT_PARAMETER, MAX_BALANCE_NQT + 1)
    );

    t.processRequest(req);
  }

  @Test
  public void processRequest_incorrectRecipient_unparsable() throws BurstException {
    final int price = 10;

    final HttpServletRequest req = QuickMocker.httpServletRequest(
        new MockParam(PRICE_NQT_PARAMETER, price),
        new MockParam(RECIPIENT_PARAMETER, "unParsable")
    );

    assertEquals(INCORRECT_RECIPIENT, t.processRequest(req));
  }

  @Test
  public void processRequest_incorrectRecipient_zero() throws BurstException {
    final int price = 10;
    final int recipientId = 0;

    final HttpServletRequest req = QuickMocker.httpServletRequest(
        new MockParam(PRICE_NQT_PARAMETER, price),
        new MockParam(RECIPIENT_PARAMETER, recipientId)
    );

    assertEquals(INCORRECT_RECIPIENT, t.processRequest(req));
  }

  @Test
  public void processRequest_incorrectAliasOwner() throws BurstException {
    final int price = 10;
    final int recipientId = 5;

    final HttpServletRequest req = QuickMocker.httpServletRequest(
        new MockParam(PRICE_NQT_PARAMETER, price),
        new MockParam(RECIPIENT_PARAMETER, recipientId)
    );

    final long aliasAccountId = 1L;
    final Alias mockAlias = mock(Alias.class);
    when(mockAlias.getAccountId()).thenReturn(aliasAccountId);

    final long mockSenderId = 2l;
    final Account mockSender = mock(Account.class);
    when(mockSender.getId()).thenReturn(mockSenderId);

    when(mockParameterService.getSenderAccount(req)).thenReturn(mockSender);
    when(mockParameterService.getAlias(req)).thenReturn(mockAlias);

    assertEquals(INCORRECT_ALIAS_OWNER, t.processRequest(req));
  }

}
