package brs.http;

import brs.peer.Hallmark;
import org.json.simple.JSONStreamAware;

import javax.servlet.http.HttpServletRequest;

import static brs.http.JSONResponses.INCORRECT_HALLMARK;
import static brs.http.JSONResponses.MISSING_HALLMARK;
import static brs.http.common.Parameters.HALLMARK_PARAMETER;

public final class DecodeHallmark extends APIServlet.APIRequestHandler {

  static final DecodeHallmark instance = new DecodeHallmark();

  private DecodeHallmark() {
    super(new APITag[] {APITag.TOKENS}, HALLMARK_PARAMETER);
  }

  @Override
  JSONStreamAware processRequest(HttpServletRequest req) {

    String hallmarkValue = req.getParameter(HALLMARK_PARAMETER);
    if (hallmarkValue == null) {
      return MISSING_HALLMARK;
    }

    try {

      Hallmark hallmark = Hallmark.parseHallmark(hallmarkValue);

      return JSONData.hallmark(hallmark);

    } catch (RuntimeException e) {
      return INCORRECT_HALLMARK;
    }
  }

}
