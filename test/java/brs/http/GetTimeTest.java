package brs.http;

import static brs.http.common.ResultFields.TIME_RESPONSE;
import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import brs.common.QuickMocker;
import brs.services.TimeService;
import javax.servlet.http.HttpServletRequest;
import org.json.simple.JSONObject;
import org.junit.Before;
import org.junit.Test;

public class GetTimeTest {

  private GetTime t;

  private TimeService mockTimeService;

  @Before
  public void setUp() {
    mockTimeService = mock(TimeService.class);

    t = new GetTime(mockTimeService);
  }

  @Test
  public void processRequest() {
    HttpServletRequest req = QuickMocker.httpServletRequest();

    final int currentEpochTime = 123;

    when(mockTimeService.getEpochTime()).thenReturn(currentEpochTime);

    final JSONObject result = (JSONObject) t.processRequest(req);

    assertEquals(currentEpochTime, result.get(TIME_RESPONSE));
  }

}
