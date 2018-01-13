package brs.common;

import static brs.http.common.ResultFields.ERROR_CODE_RESPONSE;

import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

public class JSONTestHelper {

  public static int errorCode(JSONStreamAware jsonStreamAware) {
    return (int) (((JSONObject) jsonStreamAware).get(ERROR_CODE_RESPONSE));
  }
}
