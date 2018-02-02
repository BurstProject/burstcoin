package brs.http;

import static brs.http.common.ResultFields.ALIAS_NAME_RESPONSE;
import static brs.http.common.ResultFields.BUYER_RESPONSE;
import static brs.http.common.ResultFields.PRICE_NQT_RESPONSE;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import brs.Alias;
import brs.Alias.Offer;
import brs.common.QuickMocker;
import brs.services.AliasService;
import brs.services.ParameterService;
import javax.servlet.http.HttpServletRequest;
import org.json.simple.JSONObject;
import org.junit.Before;
import org.junit.Test;

public class GetAliasTest {

  private GetAlias t;

  private ParameterService mockParameterService;
  private AliasService mockAliasService;

  @Before
  public void setUp() {
    mockParameterService = mock(ParameterService.class);
    mockAliasService = mock(AliasService.class);

    t = new GetAlias(mockParameterService, mockAliasService);
  }

  @Test
  public void processRequest() throws ParameterException {
    final Alias mockAlias = mock(Alias.class);
    when(mockAlias.getAliasName()).thenReturn("mockAliasName");

    final Offer mockOffer = mock(Offer.class);
    when(mockOffer.getPriceNQT()).thenReturn(123L);
    when(mockOffer.getBuyerId()).thenReturn(345L);

    final HttpServletRequest req = QuickMocker.httpServletRequest();

    when(mockParameterService.getAlias(eq(req))).thenReturn(mockAlias);
    when(mockAliasService.getOffer(eq(mockAlias))).thenReturn(mockOffer);

    final JSONObject result = (JSONObject) t.processRequest(req);
    assertNotNull(result);
    assertEquals(mockAlias.getAliasName(), result.get(ALIAS_NAME_RESPONSE));
    assertEquals("" + mockOffer.getPriceNQT(), result.get(PRICE_NQT_RESPONSE));
    assertEquals("" + mockOffer.getBuyerId(), result.get(BUYER_RESPONSE));
  }

}
