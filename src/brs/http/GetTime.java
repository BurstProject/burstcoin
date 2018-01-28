package brs.http;

import static brs.http.common.ResultFields.TIME_RESPONSE;

import brs.services.TimeService;
import javax.servlet.http.HttpServletRequest;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

public final class GetTime extends APIServlet.APIRequestHandler {

  private final TimeService timeService;

  GetTime(TimeService timeService) {
    super(new APITag[]{APITag.INFO});
    this.timeService = timeService;
  }

  @Override
  JSONStreamAware processRequest(HttpServletRequest req) {
    JSONObject response = new JSONObject();
    response.put(TIME_RESPONSE, timeService.getEpochTime());

    return response;
  }

}
