package brs.http;

import static brs.Constants.TOKEN;
import static brs.Constants.WEBSITE;
import static brs.http.JSONResponses.INCORRECT_WEBSITE;
import static brs.http.JSONResponses.MISSING_TOKEN;
import static brs.http.JSONResponses.MISSING_WEBSITE;
import static org.junit.Assert.assertEquals;

import brs.common.QuickMocker;
import brs.common.QuickMocker.MockParam;
import javax.servlet.http.HttpServletRequest;
import org.junit.Before;
import org.junit.Test;

public class DecodeTokenTest {

  private DecodeToken t;

  @Before
  public void setUp() {
    t = new DecodeToken();
  }

  //TODO What is a "correct" website/token combination?

  @Test
  public void processRequest_missingWebsite() {
    HttpServletRequest req = QuickMocker.httpServletRequest(
        new MockParam(WEBSITE, null)
    );
    assertEquals(MISSING_WEBSITE, t.processRequest(req));
  }

  @Test
  public void processRequest_missingToken() {
    HttpServletRequest req = QuickMocker.httpServletRequest(
        new MockParam(WEBSITE, "abc"),
        new MockParam(TOKEN, null)
    );
    assertEquals(MISSING_TOKEN, t.processRequest(req));
  }

  @Test
  public void processRequest_incorrectWebsite() {
    final HttpServletRequest req = QuickMocker.httpServletRequest(
        new MockParam(WEBSITE, "incorrectWebsite"),
        new MockParam(TOKEN, "123"));

    assertEquals(INCORRECT_WEBSITE, t.processRequest(req));
  }
}
