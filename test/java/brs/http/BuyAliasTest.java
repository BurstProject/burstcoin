package brs.http;

import static brs.http.JSONResponses.INCORRECT_ALIAS_NOTFORSALE;
import static brs.http.common.Parameters.AMOUNT_NQT_PARAMETER;
import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.eq;

import brs.Account;
import brs.Alias;
import brs.Alias.Offer;
import brs.Blockchain;
import brs.BurstException;
import brs.TransactionProcessor;
import brs.common.QuickMocker;
import brs.common.QuickMocker.MockParam;
import brs.services.AliasService;
import brs.services.ParameterService;
import javax.servlet.http.HttpServletRequest;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.powermock.modules.junit4.PowerMockRunner;

@RunWith(PowerMockRunner.class)
public class BuyAliasTest {

  private BuyAlias t;

  private ParameterService parameterServiceMock;
  private TransactionProcessor transactionProcessorMock;
  private Blockchain blockchain;
  private AliasService aliasService;

  @Before
  public void init() {
    parameterServiceMock = mock(ParameterService.class);
    transactionProcessorMock = mock(TransactionProcessor.class);
    blockchain = mock(Blockchain.class);
    aliasService = mock(AliasService.class);

    t = new BuyAlias(parameterServiceMock, transactionProcessorMock, blockchain, aliasService);
  }

  @Test
  public void processRequest() throws BurstException {
    final long mockSellerId = 123L;
    final String mockAliasName = "mockAliasName";

    final Account mockBuyerAccount = mock(Account.class);

    final Alias mockAlias = mock(Alias.class);
    when(mockAlias.getAccountId()).thenReturn(mockSellerId);
    when(mockAlias.getAliasName()).thenReturn(mockAliasName);

    final Offer mockOfferOnAlias = mock(Offer.class);

    when(aliasService.getOffer(eq(mockAlias))).thenReturn(mockOfferOnAlias);

    final HttpServletRequest req = QuickMocker.httpServletRequest(new MockParam(AMOUNT_NQT_PARAMETER, "3"));

    when(parameterServiceMock.getAccount(eq(req))).thenReturn(mockBuyerAccount);
    when(parameterServiceMock.getAlias(eq(req))).thenReturn(mockAlias);

    t.processRequest(req);
  }

  @Test
  public void processRequest_aliasNotForSale() throws BurstException {
    final HttpServletRequest req = QuickMocker.httpServletRequest(new MockParam(AMOUNT_NQT_PARAMETER, "3"));
    final Alias mockAlias = mock(Alias.class);

    when(parameterServiceMock.getAlias(eq(req))).thenReturn(mockAlias);

    when(aliasService.getOffer(eq(mockAlias))).thenReturn(null);

    assertEquals(INCORRECT_ALIAS_NOTFORSALE, t.processRequest(req));
  }

}
