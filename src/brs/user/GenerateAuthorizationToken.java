package brs.user;

import brs.Token;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;

import static brs.Constants.*;

import static brs.http.common.Parameters.SECRET_PHRASE_PARAMETER;
import static brs.user.JSONResponses.INVALID_SECRET_PHRASE;

public final class GenerateAuthorizationToken extends UserServlet.UserRequestHandler {

  static final GenerateAuthorizationToken instance = new GenerateAuthorizationToken();

  private GenerateAuthorizationToken() {}

  @Override
  JSONStreamAware processRequest(HttpServletRequest req, User user) throws IOException {
    String secretPhrase = req.getParameter(SECRET_PHRASE_PARAMETER);
    if (! user.getSecretPhrase().equals(secretPhrase)) {
      return INVALID_SECRET_PHRASE;
    }

    String tokenString = Token.generateToken(secretPhrase, req.getParameter(WEBSITE).trim());

    JSONObject response = new JSONObject();
    response.put(RESPONSE, "showAuthorizationToken");
    response.put(TOKEN, tokenString);

    return response;
  }

  @Override
  boolean requirePost() {
    return true;
  }

}
