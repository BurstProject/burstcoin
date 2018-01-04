package brs.http;

import org.junit.Test;

import javax.servlet.http.HttpServletRequest;

import static brs.http.JSONResponses.MISSING_SECRET_PHRASE;
import static brs.http.JSONResponses.UNKNOWN_ACCOUNT;
import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class StartForgingTest {

    private StartForging t = StartForging.instance;

    private static final String SECRET_PHRASE_PARAMETER = "secretPhrase";
    private static final String TEST_SECRET_PHRASE = "Secrets!";

    @Test
    public void processRequest_Test() {
        final HttpServletRequest requestWithoutSecretPhrase = mock(HttpServletRequest.class);

        when(requestWithoutSecretPhrase.getParameter(eq(SECRET_PHRASE_PARAMETER))).thenReturn(TEST_SECRET_PHRASE);

        assertEquals(UNKNOWN_ACCOUNT, t.processRequest(requestWithoutSecretPhrase));
    }

    @Test
    public void processRequest_secretPhraseParameterNotSetGivesMissingSecretPhraseTest() {
        final HttpServletRequest requestWithoutSecretPhrase = mock(HttpServletRequest.class);

        when(requestWithoutSecretPhrase.getParameter(eq(SECRET_PHRASE_PARAMETER))).thenReturn(null);

        assertEquals(MISSING_SECRET_PHRASE, t.processRequest(requestWithoutSecretPhrase));
    }

    @Test
    public void requirePost_Test() {
        assertEquals(true, t.requirePost());
    }
}
