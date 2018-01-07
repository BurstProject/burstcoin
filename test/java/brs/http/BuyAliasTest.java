package brs.http;

import static brs.http.JSONResponses.INCORRECT_ALIAS_NOTFORSALE;
import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.eq;
import static org.powermock.api.mockito.PowerMockito.mock;
import static org.powermock.api.mockito.PowerMockito.when;

import brs.Alias;
import brs.BurstException;
import javax.servlet.http.HttpServletRequest;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

@RunWith(PowerMockRunner.class)
@PrepareForTest({ParameterParser.class, Alias.class})
public class BuyAliasTest {

  private BuyAlias t;

  @Before
  public void init() {
    t = BuyAlias.instance;
  }

  @Test
  public void processRequest() {
    //TODO implement happy path test
  }

  @Test
  public void processRequest_aliasNotForSale() throws BurstException {
    PowerMockito.mockStatic(ParameterParser.class);
    PowerMockito.mockStatic(Alias.class);

    final HttpServletRequest req = mock(HttpServletRequest.class);
    final Alias mockAlias = mock(Alias.class);

    when(ParameterParser.getAlias(eq(req))).thenReturn(mockAlias);

    when(Alias.getOffer(eq(mockAlias))).thenReturn(null);

    assertEquals(INCORRECT_ALIAS_NOTFORSALE, t.processRequest(req));
  }

}
