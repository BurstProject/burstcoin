package brs.common;

import static brs.http.JSONResponses.MISSING_FEE;
import static brs.http.common.Parameters.DEADLINE_PARAMETER;
import static brs.http.common.Parameters.FEE_QT_PARAMETER;
import static brs.http.common.Parameters.PUBLIC_KEY_PARAMETER;
import static brs.http.common.Parameters.SECRET_PHRASE_PARAMETER;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import javax.servlet.http.HttpServletRequest;
import org.mockito.Mock;

public class QuickMocker {

  public static HttpServletRequest httpServletRequest(MockParam... parameters) {
    final HttpServletRequest mockedRequest = mock(HttpServletRequest.class);

    for (MockParam mp : parameters) {
      when(mockedRequest.getParameter(mp.key)).thenReturn(mp.value);
    }

    return mockedRequest;
  }

  public static HttpServletRequest httpServletRequestDefaultKeys(MockParam... parameters) {
    final List<MockParam> paramsWithKeys = new ArrayList<>(Arrays.asList(
        new MockParam(SECRET_PHRASE_PARAMETER, TestConstants.TEST_SECRET_PHRASE),
        new MockParam(PUBLIC_KEY_PARAMETER, TestConstants.TEST_PUBLIC_KEY),
        new MockParam(DEADLINE_PARAMETER, TestConstants.DEADLINE),
        new MockParam(FEE_QT_PARAMETER, TestConstants.FEE)
    ));

    paramsWithKeys.addAll(Arrays.asList(parameters));

    return httpServletRequest(paramsWithKeys.toArray(new MockParam[paramsWithKeys.size()]));
  }

  public static class MockParam {

    private final String key;
    private final String value;

    public MockParam(String key, String value) {
      this.key = key;
      this.value = value;
    }

    public MockParam(String key, int value) {
      this(key, "" + value);
    }

    public MockParam(String key, long value) {
      this(key, "" + value);
    }

    public MockParam(String key, boolean value) {
      this(key, "" + value);
    }
  }
}
