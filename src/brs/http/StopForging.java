package brs.http;

import brs.Generator;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

import javax.servlet.http.HttpServletRequest;

import static brs.http.JSONResponses.MISSING_SECRET_PHRASE;
import static brs.http.common.Parameters.SECRET_PHRASE_PARAMETER;


public final class StopForging extends APIServlet.APIRequestHandler {

  static final StopForging instance = new StopForging();

  private StopForging() {
    super(new APITag[] {APITag.FORGING}, SECRET_PHRASE_PARAMETER);
  }

  @Override
  JSONStreamAware processRequest(HttpServletRequest req) {

    String secretPhrase = req.getParameter(SECRET_PHRASE_PARAMETER);
    if (secretPhrase == null) {
      return MISSING_SECRET_PHRASE;
    }

    //Generator generator = Generator.stopForging(secretPhrase);
    Generator.GeneratorState generator = null;

    JSONObject response = new JSONObject();
    response.put("foundAndStopped", generator != null);
    return response;

  }

  @Override
  boolean requirePost() {
    return true;
  }

}
