package brs.util;

import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;
import java.io.IOException;
import java.io.Writer;

import static brs.Constants.*;

public final class JSON {

  private JSON() {} //never

  public static final JSONStreamAware emptyJSON = prepare(new JSONObject());

  public static JSONStreamAware prepare(final JSONObject json) {
    return new JSONStreamAware() {
      private final char[] jsonChars = json.toJSONString().toCharArray();
      @Override
      public void writeJSONString(Writer out) throws IOException {
        out.write(jsonChars);
      }
    };
  }

  public static JSONStreamAware prepareRequest(final JSONObject json) {
    json.put(PROTOCOL, "B1");
    return prepare(json);
  }

}
