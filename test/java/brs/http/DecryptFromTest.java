package brs.http;

import static brs.common.TestConstants.TEST_PUBLIC_KEY;
import static brs.common.TestConstants.TEST_PUBLIC_KEY_BYTES;
import static brs.common.TestConstants.TEST_SECRET_PHRASE;
import static brs.http.JSONResponses.DECRYPTION_FAILED;
import static brs.http.JSONResponses.INCORRECT_ACCOUNT;
import static brs.http.common.Parameters.DATA_PARAMETER;
import static brs.http.common.Parameters.DECRYPTED_MESSAGE_IS_TEXT_PARAMETER;
import static brs.http.common.Parameters.NONCE_PARAMETER;
import static brs.http.common.Parameters.SECRET_PHRASE_PARAMETER;
import static brs.http.common.ResultFields.DECRYPTED_MESSAGE_RESPONSE;
import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import brs.Account;
import brs.BurstException;
import brs.common.QuickMocker;
import brs.common.QuickMocker.MockParam;
import brs.common.TestConstants;
import brs.crypto.EncryptedData;
import brs.services.ParameterService;
import javax.servlet.http.HttpServletRequest;
import org.json.simple.JSONObject;
import org.junit.Before;
import org.junit.Test;

public class DecryptFromTest {

  private DecryptFrom t;

  private ParameterService mockParameterService;

  @Before
  public void setUp() {
    mockParameterService = mock(ParameterService.class);

    t = new DecryptFrom(mockParameterService);
  }

  @Test
  public void processRequest() throws BurstException {
    final HttpServletRequest req = QuickMocker.httpServletRequest(
        new MockParam(SECRET_PHRASE_PARAMETER, TEST_SECRET_PHRASE),
        new MockParam(DECRYPTED_MESSAGE_IS_TEXT_PARAMETER, "true"),
        new MockParam(DATA_PARAMETER, "abc"),
        new MockParam(NONCE_PARAMETER, "def")
    );

    final Account mockAccount = mock(Account.class);

    when(mockAccount.decryptFrom(any(EncryptedData.class), eq(TEST_SECRET_PHRASE)))
        .thenReturn(new byte[]{(byte) 1});

    when(mockAccount.getPublicKey()).thenReturn(TEST_PUBLIC_KEY_BYTES);

    when(mockParameterService.getAccount(req)).thenReturn(mockAccount);

    assertEquals("", ((JSONObject) t.processRequest(req)).get(DECRYPTED_MESSAGE_RESPONSE));
  }

  @Test
  public void processRequest_accountWithoutPublicKeyIsIncorrectAccount() throws BurstException {
    final HttpServletRequest req = QuickMocker.httpServletRequest();

    when(mockParameterService.getAccount(req)).thenReturn(mock(Account.class));

    assertEquals(INCORRECT_ACCOUNT, t.processRequest(req));
  }

}
