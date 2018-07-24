package brs.http;

import static brs.TransactionType.AdvancedPayment.SUBSCRIPTION_CANCEL;
import static brs.fluxcapacitor.FeatureToggle.DIGITAL_GOODS_STORE;
import static brs.http.common.Parameters.SUBSCRIPTION_PARAMETER;
import static brs.http.common.ResultFields.ERROR_CODE_RESPONSE;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.powermock.api.mockito.PowerMockito.mockStatic;

import brs.Account;
import brs.Attachment;
import brs.Blockchain;
import brs.Burst;
import brs.BurstException;
import brs.Subscription;
import brs.common.QuickMocker;
import brs.common.QuickMocker.MockParam;
import brs.fluxcapacitor.FluxCapacitor;
import brs.services.ParameterService;
import brs.services.SubscriptionService;
import javax.servlet.http.HttpServletRequest;
import org.json.simple.JSONObject;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

@RunWith(PowerMockRunner.class)
@PrepareForTest(Burst.class)
public class SubscriptionCancelTest extends AbstractTransactionTest {

  private SubscriptionCancel t;

  private ParameterService parameterServiceMock;
  private SubscriptionService subscriptionServiceMock;
  private Blockchain blockchainMock;
  private APITransactionManager apiTransactionManagerMock;

  @Before
  public void setUp() {
    parameterServiceMock = mock(ParameterService.class);
    subscriptionServiceMock = mock(SubscriptionService.class);
    blockchainMock = mock(Blockchain.class);
    apiTransactionManagerMock = mock(APITransactionManager.class);

    t = new SubscriptionCancel(parameterServiceMock, subscriptionServiceMock, blockchainMock, apiTransactionManagerMock);
  }

  @Test
  public void processRequest() throws BurstException {
    final Long subscriptionIdParameter = 123L;

    final HttpServletRequest req = QuickMocker.httpServletRequest(
        new MockParam(SUBSCRIPTION_PARAMETER, subscriptionIdParameter)
    );

    final Account mockSender = mock(Account.class);
    when(mockSender.getId()).thenReturn(1L);

    final Subscription mockSubscription = mock(Subscription.class);
    when(mockSubscription.getId()).thenReturn(subscriptionIdParameter);
    when(mockSubscription.getSenderId()).thenReturn(1L);
    when(mockSubscription.getRecipientId()).thenReturn(2L);

    when(parameterServiceMock.getSenderAccount(eq(req))).thenReturn(mockSender);
    when(subscriptionServiceMock.getSubscription(eq(subscriptionIdParameter))).thenReturn(mockSubscription);

    mockStatic(Burst.class);
    final FluxCapacitor fluxCapacitor = QuickMocker.fluxCapacitorEnabledFunctionalities(DIGITAL_GOODS_STORE);
    when(Burst.getFluxCapacitor()).thenReturn(fluxCapacitor);

    final Attachment.AdvancedPaymentSubscriptionCancel attachment = (Attachment.AdvancedPaymentSubscriptionCancel) attachmentCreatedTransaction(() -> t.processRequest(req), apiTransactionManagerMock);
    assertNotNull(attachment);

    assertEquals(SUBSCRIPTION_CANCEL, attachment.getTransactionType());
    assertEquals(subscriptionIdParameter, attachment.getSubscriptionId());
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

    when(subscriptionServiceMock.getSubscription(eq(subscriptionId))).thenReturn(null);

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

    when(parameterServiceMock.getSenderAccount(eq(req))).thenReturn(mockSender);
    when(subscriptionServiceMock.getSubscription(eq(subscriptionId))).thenReturn(mockSubscription);

    final JSONObject response = (JSONObject) t.processRequest(req);
    assertNotNull(response);

    assertEquals(7, response.get(ERROR_CODE_RESPONSE));
  }
}
