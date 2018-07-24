package brs.http;

import static brs.http.common.ResultFields.CHEAP_FEE_RESPONSE;
import static brs.http.common.ResultFields.STANDARD_FEE_RESPONSE;
import static brs.http.common.ResultFields.PRIORITY_FEE_RESPONSE;

import brs.feesuggestions.FeeSuggestion;
import brs.feesuggestions.FeeSuggestionCalculator;
import javax.servlet.http.HttpServletRequest;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

public class SuggestFee extends APIServlet.APIRequestHandler {

  private final FeeSuggestionCalculator feeSuggestionCalculator;

  public SuggestFee(FeeSuggestionCalculator feeSuggestionCalculator) {
    super(new APITag[]{APITag.FEES});
    this.feeSuggestionCalculator = feeSuggestionCalculator;
  }

  @Override
  JSONStreamAware processRequest(HttpServletRequest req) {
    final FeeSuggestion feeSuggestion = feeSuggestionCalculator.giveFeeSuggestion();

    final JSONObject response = new JSONObject();

    response.put(CHEAP_FEE_RESPONSE, feeSuggestion.getCheapFee());
    response.put(STANDARD_FEE_RESPONSE, feeSuggestion.getStandardFee());
    response.put(PRIORITY_FEE_RESPONSE, feeSuggestion.getPriorityFee());

    return response;
  }

}
