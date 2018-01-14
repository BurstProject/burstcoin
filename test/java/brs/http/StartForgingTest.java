package brs.http;

import static brs.common.TestConstants.TEST_SECRET_PHRASE;
import static brs.http.JSONResponses.MISSING_SECRET_PHRASE;
import static brs.http.JSONResponses.UNKNOWN_ACCOUNT;
import static brs.http.common.Parameters.SECRET_PHRASE_PARAMETER;
import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import javax.servlet.http.HttpServletRequest;
import org.junit.Before;
import org.junit.Test;

public class StartForgingTest {

  private StartForging t;

  @Before
  public void setUp() {
    t = StartForging.instance;
  }

  @Test
  public void processRequest() {
    final HttpServletRequest requestWithoutSecretPhrase = mock(HttpServletRequest.class);

    when(requestWithoutSecretPhrase.getParameter(eq(SECRET_PHRASE_PARAMETER))).thenReturn(TEST_SECRET_PHRASE);

    assertEquals(UNKNOWN_ACCOUNT, t.processRequest(requestWithoutSecretPhrase));
  }

  @Test
  public void processRequest_secretPhraseParameterNotSetGivesMissingSecretPhrase() {
    final HttpServletRequest requestWithoutSecretPhrase = mock(HttpServletRequest.class);

    when(requestWithoutSecretPhrase.getParameter(eq(SECRET_PHRASE_PARAMETER))).thenReturn(null);

    assertEquals(MISSING_SECRET_PHRASE, t.processRequest(requestWithoutSecretPhrase));
  }

  @Test
  public void requirePost_Test() {
    assertEquals(true, t.requirePost());
  }
}
