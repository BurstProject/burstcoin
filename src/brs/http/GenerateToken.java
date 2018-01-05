package brs.http;

import brs.Token;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;
import javax.servlet.http.HttpServletRequest;

import static brs.http.JSONResponses.*;
import static brs.Constants.*;

public final class GenerateToken extends APIServlet.APIRequestHandler {

  static final GenerateToken instance = new GenerateToken();

  private GenerateToken() {
    super(new APITag[] {APITag.TOKENS}, WEBSITE, "secretPhrase");
  }

  @Override
  JSONStreamAware processRequest(HttpServletRequest req) {

    String secretPhrase = req.getParameter("secretPhrase");
    String website = req.getParameter(WEBSITE);
    if (secretPhrase == null) {
      return MISSING_SECRET_PHRASE;
    } else if (website == null) {
      return MISSING_WEBSITE;
    }

    try {

      String tokenString = Token.generateToken(secretPhrase, website.trim());

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
