package brs.http;

import static brs.http.common.ResultFields.AT_IDS_RESPONSE;

import brs.services.ATService;
import brs.util.Convert;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

import javax.servlet.http.HttpServletRequest;

public final class GetATIds extends APIServlet.APIRequestHandler {

  private final ATService atService;

  GetATIds(ATService atService) {
    super(new APITag[] {APITag.AT});
    this.atService = atService;
  }

  @Override
  JSONStreamAware processRequest(HttpServletRequest req) {

    JSONArray atIds = new JSONArray();
    for (Long id : atService.getAllATIds()) {
      atIds.add(Convert.toUnsignedLong(id));
    }

    JSONObject response = new JSONObject();
    response.put(AT_IDS_RESPONSE, atIds);
    return response;
  }

}
