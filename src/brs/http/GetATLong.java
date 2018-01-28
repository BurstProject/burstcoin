package brs.http;

import static brs.http.common.Parameters.HEX_STRING_PARAMETER;

import javax.servlet.http.HttpServletRequest;
import org.json.simple.JSONStreamAware;

public final class GetATLong extends APIServlet.APIRequestHandler {

  static final GetATLong instance = new GetATLong();

  private GetATLong() {
    super(new APITag[] {APITag.AT}, HEX_STRING_PARAMETER);
  }

  @Override
  JSONStreamAware processRequest(HttpServletRequest req) {
    return JSONData.hex2long(ParameterParser.getATLong(req));
  }

}
