package brs.http;

import brs.Token;
import org.json.simple.JSONStreamAware;
import javax.servlet.http.HttpServletRequest;

import static brs.http.JSONResponses.*;
import static brs.Constants.*;

public final class DecodeToken extends APIServlet.APIRequestHandler {

  public DecodeToken() {
    super(new APITag[] {APITag.TOKENS}, WEBSITE, TOKEN);
  }

  @Override
  public JSONStreamAware processRequest(HttpServletRequest req) {
    String website = req.getParameter(WEBSITE);
    String tokenString = req.getParameter(TOKEN);

    if (website == null) {
      return MISSING_WEBSITE;
    } else if (tokenString == null) {
      return MISSING_TOKEN;
    }

    try {
      Token token = Token.parseToken(tokenString, website.trim());
      return JSONData.token(token);
    } catch (RuntimeException e) {
      return INCORRECT_WEBSITE;
    }
  }
}
