package brs.http;

import brs.Token;
import brs.services.TimeService;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;
import javax.servlet.http.HttpServletRequest;

import static brs.http.JSONResponses.*;
import static brs.Constants.*;
import static brs.http.common.Parameters.SECRET_PHRASE_PARAMETER;

public final class GenerateToken extends APIServlet.APIRequestHandler {

  private final TimeService timeService;

  GenerateToken(TimeService timeService) {
    super(new APITag[] {APITag.TOKENS}, WEBSITE, SECRET_PHRASE_PARAMETER);
    this.timeService = timeService;
  }

  @Override
  JSONStreamAware processRequest(HttpServletRequest req) {

    String secretPhrase = req.getParameter(SECRET_PHRASE_PARAMETER);
    String website = req.getParameter(WEBSITE);
    if (secretPhrase == null) {
      return MISSING_SECRET_PHRASE;
    } else if (website == null) {
      return MISSING_WEBSITE;
    }

    try {

      String tokenString = Token.generateToken(secretPhrase, website.trim(), timeService.getEpochTime());

      JSONObject response = new JSONObject();
      response.put(TOKEN, tokenString);

      return response;

    } catch (RuntimeException e) {
      return INCORRECT_WEBSITE;
    }

  }

  @Override
  boolean requirePost() {
    return true;
  }

}
