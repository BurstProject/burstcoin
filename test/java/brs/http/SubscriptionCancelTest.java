package brs.http;

import static brs.http.common.Parameters.SUBSCRIPTION_PARAMETER;
import static brs.http.common.ResultFields.ERROR_CODE_RESPONSE;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import brs.Account;
import brs.Blockchain;
import brs.BurstException;
import brs.Subscription;
import brs.TransactionProcessor;
import brs.common.QuickMocker;
import brs.common.QuickMocker.MockParam;
import brs.services.AccountService;
import brs.services.ParameterService;
import brs.services.SubscriptionService;
import brs.services.TransactionService;
import javax.servlet.http.HttpServletRequest;
import org.json.simple.JSONObject;
import org.junit.Before;
import org.junit.Test;

public class SubscriptionCancelTest extends AbstractTransactionTest {

  private SubscriptionCancel t;

  private ParameterService mockParameterService;
  private SubscriptionService mockSubscriptionService;
  private AccountService mockAccountService;
  private Blockchain mockBlockchain;
  private TransactionProcessor mockTransactionProcessor;
  private TransactionService transactionServiceMock;

  @Before
  public void setUp() {
    mockParameterService = mock(ParameterService.class);
    mockSubscriptionService = mock(SubscriptionService.class);
    mockAccountService = mock(AccountService.class);
    mockBlockchain = mock(Blockchain.class);
    mockTransactionProcessor = mock(TransactionProcessor.class);
    transactionServiceMock = mock(TransactionService.class);

    t = new SubscriptionCancel(mockParameterService, mockTransactionProcessor, mockBlockchain, mockAccountService, mockSubscriptionService, transactionServiceMock);
  }

  @Test
  public void processRequest() throws BurstException {
    final long subscriptionId = 123L;

    final HttpServletRequest req = QuickMocker.httpServletRequest(
        new MockParam(SUBSCRIPTION_PARAMETER, subscriptionId)
    );

    final Account mockSender = mock(Account.class);
    when(mockSender.getId()).thenReturn(1L);

    final Subscription mockSubscription = mock(Subscription.class);
    when(mockSubscription.getSenderId()).thenReturn(1L);
    when(mockSubscription.getRecipientId()).thenReturn(2L);

    when(mockParameterService.getSenderAccount(eq(req))).thenReturn(mockSender);
    when(mockSubscriptionService.getSubscription(eq(subscriptionId))).thenReturn(mockSubscription);

    t.processRequest(req);
  }

  @Test
  public void processRequest_missingSubscriptionParameter() throws BurstException {
    final HttpServletRequest req = QuickMocker.httpServletRequest();

    final JSONObject response = (JSONObject) t.processRequest(req);
    assertNotNull(response);

    assertEquals(3, response.get(ERROR_CODE_RESPONSE));
  }

  @Test
  public void processRequest_failedToParseSubscription() throws BurstException {
    final HttpServletRequest req = QuickMocker.httpServletRequest(
      new MockParam(SUBSCRIPTION_PARAMETER, "notALong")
    );

    final JSONObject response = (JSONObject) t.processRequest(req);
    assertNotNull(response);

    assertEquals(4, response.get(ERROR_CODE_RESPONSE));
  }

  @Test
  public void processRequest_subscriptionNotFound() throws BurstException {
    final long subscriptionId = 123L;

    final HttpServletRequest req = QuickMocker.httpServletRequest(
        new MockParam(SUBSCRIPTION_PARAMETER, subscriptionId)
    );

    when(mockSubscriptionService.getSubscription(eq(subscriptionId))).thenReturn(null);

    final JSONObject response = (JSONObject) t.processRequest(req);
    assertNotNull(response);

    assertEquals(5, response.get(ERROR_CODE_RESPONSE));
  }

  @Test
  public void processRequest_userIsNotSenderOrRecipient() throws BurstException {
    final long subscriptionId = 123L;

    final HttpServletRequest req = QuickMocker.httpServletRequest(
        new MockParam(SUBSCRIPTION_PARAMETER, subscriptionId)
    );

    final Account mockSender = mock(Account.class);
    when(mockSender.getId()).thenReturn(1L);

    final Subscription mockSubscription = mock(Subscription.class);
    when(mockSubscription.getSenderId()).thenReturn(2L);
    when(mockSubscription.getRecipientId()).thenReturn(3L);

    when(mockParameterService.getSenderAccount(eq(req))).thenReturn(mockSender);
    when(mockSubscriptionService.getSubscription(eq(subscriptionId))).thenReturn(mockSubscription);

    final JSONObject response = (JSONObject) t.processRequest(req);
    assertNotNull(response);

    assertEquals(7, response.get(ERROR_CODE_RESPONSE));
  }
}
