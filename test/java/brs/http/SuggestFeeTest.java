package brs.http;

import static brs.Constants.FEE_QUANT;
import static brs.http.common.ResultFields.CHEAP_FEE_RESPONSE;
import static brs.http.common.ResultFields.OPTIMUM_FEE_RESPONSE;
import static brs.http.common.ResultFields.PRIORITY_FEE_RESPONSE;
import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import brs.BurstException;
import brs.common.QuickMocker;
import brs.feesuggestions.FeeSuggestion;
import brs.feesuggestions.FeeSuggestionCalculator;
import javax.servlet.http.HttpServletRequest;
import org.json.simple.JSONObject;
import org.junit.Before;
import org.junit.Test;

public class SuggestFeeTest {

  private SuggestFee t;

  private FeeSuggestionCalculator feeSuggestionCalculator;

  @Before
  public void setUp() {
    feeSuggestionCalculator = mock(FeeSuggestionCalculator.class);

    t = new SuggestFee(feeSuggestionCalculator);
  }

  @Test
  public void processRequest() throws BurstException {
    final HttpServletRequest req = QuickMocker.httpServletRequest();

    final long cheap = 1 * FEE_QUANT;
    final long optimum = 5 * FEE_QUANT;
    final long priority = 10 * FEE_QUANT;
    final FeeSuggestion feeSuggestion = new FeeSuggestion(cheap, optimum, priority);

    when(feeSuggestionCalculator.giveFeeSuggestion()).thenReturn(feeSuggestion);

    final JSONObject result = (JSONObject) t.processRequest(req);

    assertEquals(cheap, result.get(CHEAP_FEE_RESPONSE));
    assertEquals(optimum, result.get(OPTIMUM_FEE_RESPONSE));
    assertEquals(priority, result.get(PRIORITY_FEE_RESPONSE));
  }
}