package brs.http;

import org.json.simple.JSONStreamAware;

import javax.servlet.http.HttpServletRequest;

import static brs.http.JSONResponses.MISSING_SECRET_PHRASE;
import static brs.http.JSONResponses.UNKNOWN_ACCOUNT;
import static brs.http.common.Parameters.SECRET_PHRASE_PARAMETER;


public final class StartForging extends APIServlet.APIRequestHandler {

  static final StartForging instance = new StartForging();

  private StartForging() {
    super(new APITag[] {APITag.FORGING}, SECRET_PHRASE_PARAMETER);
  }

  @Override
  JSONStreamAware processRequest(HttpServletRequest req) {

    String secretPhrase = req.getParameter(SECRET_PHRASE_PARAMETER);
    if (secretPhrase == null) {
      return MISSING_SECRET_PHRASE;
    }

    //Generator generator = Generator.startForging(secretPhrase);
    //if (generator == null) {
    return UNKNOWN_ACCOUNT;
    //}

    //JSONObject response = new JSONObject();
    //response.put("deadline", generator.getDeadline());
    //return response;

  }

  @Override
  boolean requirePost() {
    return true;
  }

}
