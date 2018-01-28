package brs.http;

import static brs.http.common.ResultFields.ALIASES_RESPONSE;
import static brs.http.common.ResultFields.ALIAS_RESPONSE;
import static brs.http.common.ResultFields.PRICE_NQT_RESPONSE;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Matchers.anyList;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import brs.Account;
import brs.Alias;
import brs.Alias.Offer;
import brs.BurstException;
import brs.common.AbstractUnitTest;
import brs.common.QuickMocker;
import brs.db.BurstIterator;
import brs.services.AliasService;
import brs.services.ParameterService;
import javax.servlet.http.HttpServletRequest;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.junit.Before;
import org.junit.Test;

public class GetAliasesTest extends AbstractUnitTest {

  private GetAliases t;

  private ParameterService mockParameterService;
  private AliasService mockAliasService;

  @Before
  public void setUp() {
    mockParameterService = mock(ParameterService.class);
    mockAliasService = mock(AliasService.class);

    t = new GetAliases(mockParameterService, mockAliasService);
  }

  @Test
  public void processRequest() throws BurstException {
    final long accountId = 123L;
    final HttpServletRequest req = QuickMocker.httpServletRequest();

    final Account mockAccount = mock(Account.class);
    when(mockAccount.getId()).thenReturn(accountId);

    final Alias mockAlias = mock(Alias.class);
    when(mockAlias.getId()).thenReturn(567L);

    final Offer mockOffer = mock(Offer.class);
    when(mockOffer.getPriceNQT()).thenReturn(234L);

    final BurstIterator<Alias> mockAliasIterator = mockBurstIterator(mockAlias);

    when(mockParameterService.getAccount(eq(req))).thenReturn(mockAccount);

    when(mockAliasService.getAliasesByOwner(eq(accountId), eq(0), eq(-1))).thenReturn(mockAliasIterator);
    when(mockAliasService.getOffer(eq(mockAlias))).thenReturn(mockOffer);

    final JSONObject resultOverview = (JSONObject) t.processRequest(req);
    assertNotNull(resultOverview);

    final JSONArray resultList = (JSONArray) resultOverview.get(ALIASES_RESPONSE);
    assertNotNull(resultList);
    assertEquals(1, resultList.size());

    final JSONObject result = (JSONObject) resultList.get(0);
    assertNotNull(result);
    assertEquals("" +mockAlias.getId(), result.get(ALIAS_RESPONSE));
    assertEquals("" + mockOffer.getPriceNQT(), result.get(PRICE_NQT_RESPONSE));
  }

}
