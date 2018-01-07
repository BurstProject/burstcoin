package brs.common;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import javax.servlet.http.HttpServletRequest;

public class QuickMocker {

  public static HttpServletRequest httpServletRequest(MockParam... parameters) {
    final HttpServletRequest mockedRequest = mock(HttpServletRequest.class);

    for (MockParam mp : parameters) {
      when(mockedRequest.getParameter(mp.key)).thenReturn(mp.value);
    }

    return mockedRequest;
  }

  public static class MockParam {

    private final String key;
    private final String value;

    public MockParam(String key, String value) {
      this.key = key;
      this.value = value;
    }
  }
}
