package brs.services.impl;

import static brs.http.common.Parameters.ACCOUNT_PARAMETER;
import static brs.http.common.Parameters.ALIAS_NAME_PARAMETER;
import static brs.http.common.Parameters.ALIAS_PARAMETER;
import static brs.http.common.Parameters.ASSET_PARAMETER;
import static brs.http.common.Parameters.PUBLIC_KEY_PARAMETER;
import static brs.http.common.Parameters.SECRET_PHRASE_PARAMETER;
import static org.junit.Assert.*;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.powermock.api.mockito.PowerMockito.when;

import brs.Account;
import brs.Alias;
import brs.Asset;
import brs.BurstException;
import brs.common.QuickMocker;
import brs.common.QuickMocker.MockParam;
import brs.common.TestConstants;
import brs.crypto.Crypto;
import brs.http.ParameterException;
import brs.services.AccountService;
import brs.services.AliasService;
import brs.services.AssetService;
import brs.util.Convert;
import com.sun.xml.internal.bind.v2.model.annotation.Quick;
import java.util.List;
import javax.servlet.http.HttpServletRequest;
import org.jooq.Param;
import org.junit.Before;
import org.junit.Test;

public class ParameterServiceImplTest {

  private ParameterServiceImpl t;

  private AccountService accountServiceMock;
  private AliasService aliasServiceMock;
  private AssetService assetServiceMock;

  @Before
  public void setUp() {
    accountServiceMock = mock(AccountService.class);
    aliasServiceMock = mock(AliasService.class);
    assetServiceMock = mock(AssetService.class);

    t = new ParameterServiceImpl(accountServiceMock, aliasServiceMock, assetServiceMock);
  }

  @Test
  public void getAccount() throws BurstException {
    final String accountId = "123";
    final Account mockAccount = mock(Account.class);

    final HttpServletRequest req = QuickMocker.httpServletRequest(new MockParam(ACCOUNT_PARAMETER, accountId));

    when(accountServiceMock.getAccount(eq(123L))).thenReturn(mockAccount);

    assertEquals(mockAccount, t.getAccount(req));
  }

  @Test(expected = ParameterException.class)
  public void getAccount_MissingAccountWhenNoAccountParameterGiven() throws BurstException {
    final HttpServletRequest req = QuickMocker.httpServletRequest();
    t.getAccount(req);
  }

  @Test(expected = ParameterException.class)
  public void getAccount_UnknownAccountWhenIdNotFound() throws BurstException {
    final String accountId = "123";
    final HttpServletRequest req = QuickMocker.httpServletRequest(new MockParam(ACCOUNT_PARAMETER, accountId));

    when(accountServiceMock.getAccount(eq(123L))).thenReturn(null);

    t.getAccount(req);
  }

  @Test(expected = ParameterException.class)
  public void getAccount_IncorrectAccountWhenRuntimeExceptionOccurs() throws BurstException {
    final String accountId = "123";
    final HttpServletRequest req = QuickMocker.httpServletRequest(new MockParam(ACCOUNT_PARAMETER, accountId));

    when(accountServiceMock.getAccount(eq(123L))).thenThrow(new RuntimeException());

    t.getAccount(req);
  }

  @Test
  public void getAccounts() throws ParameterException {
    final HttpServletRequest req = QuickMocker.httpServletRequest();
    final String accountID1 = "123";
    final String accountID2 = "321";
    final String[] accountIds = new String[]{accountID1, accountID2};

    when(req.getParameterValues(eq(ACCOUNT_PARAMETER))).thenReturn(accountIds);

    final Account mockAccount1 = mock(Account.class);
    final Account mockAccount2 = mock(Account.class);

    when(accountServiceMock.getAccount(eq(123L))).thenReturn(mockAccount1);
    when(accountServiceMock.getAccount(eq(321L))).thenReturn(mockAccount2);

    final List<Account> result = t.getAccounts(req);

    assertNotNull(result);
    assertEquals(2, result.size());
    assertEquals(mockAccount1, result.get(0));
    assertEquals(mockAccount2, result.get(1));
  }

  @Test
  public void getAccounts_emptyResultWhenEmptyAccountValueGiven() throws ParameterException {
    final HttpServletRequest req = QuickMocker.httpServletRequest();
    final String[] accountIds = new String[]{""};

    when(req.getParameterValues(eq(ACCOUNT_PARAMETER))).thenReturn(accountIds);

    final List<Account> result = t.getAccounts(req);

    assertNotNull(result);
    assertTrue(result.isEmpty());
  }

  @Test
  public void getAccounts_emptyResultWhenNullAccountValueGiven() throws ParameterException {
    final HttpServletRequest req = QuickMocker.httpServletRequest();
    final String[] accountIds = new String[]{null};

    when(req.getParameterValues(eq(ACCOUNT_PARAMETER))).thenReturn(accountIds);

    final List<Account> result = t.getAccounts(req);

    assertNotNull(result);
    assertTrue(result.isEmpty());
  }


  @Test(expected = ParameterException.class)
  public void getAccounts_missingAccountWhenNoParametersNull() throws ParameterException {
    final HttpServletRequest req = QuickMocker.httpServletRequest();
    when(req.getParameterValues(eq(ACCOUNT_PARAMETER))).thenReturn(null);

    t.getAccounts(req);
  }

  @Test(expected = ParameterException.class)
  public void getAccounts_missingAccountWhenNoParametersGiven() throws ParameterException {
    final HttpServletRequest req = QuickMocker.httpServletRequest();
    final String[] accountIds = new String[0];

    when(req.getParameterValues(eq(ACCOUNT_PARAMETER))).thenReturn(accountIds);

    t.getAccounts(req);
  }

  @Test(expected = ParameterException.class)
  public void getAccounts_unknownAccountWhenNotFound() throws ParameterException {
    final HttpServletRequest req = QuickMocker.httpServletRequest();
    final String accountID1 = "123";
    final String[] accountIds = new String[]{accountID1};

    when(req.getParameterValues(eq(ACCOUNT_PARAMETER))).thenReturn(accountIds);

    when(accountServiceMock.getAccount(eq(123L))).thenReturn(null);

    t.getAccounts(req);
  }

  @Test(expected = ParameterException.class)
  public void getAccounts_incorrectAccountWhenRuntimeExceptionOccurs() throws ParameterException {
    final HttpServletRequest req = QuickMocker.httpServletRequest();
    final String accountID1 = "123";
    final String[] accountIds = new String[]{accountID1};

    when(req.getParameterValues(eq(ACCOUNT_PARAMETER))).thenReturn(accountIds);

    when(accountServiceMock.getAccount(eq(123L))).thenThrow(new RuntimeException());

    t.getAccounts(req);
  }

  @Test
  public void getSenderAccount_withSecretPhrase() throws ParameterException {
    final String secretPhrase = TestConstants.TEST_SECRET_PHRASE;
    final HttpServletRequest req = QuickMocker.httpServletRequest(new MockParam(SECRET_PHRASE_PARAMETER, secretPhrase));

    final Account mockAccount = mock(Account.class);

    when(accountServiceMock.getAccount(eq(Crypto.getPublicKey(secretPhrase)))).thenReturn(mockAccount);

    assertEquals(mockAccount, t.getSenderAccount(req));
  }

  @Test
  public void getSenderAccount_withPublicKey() throws ParameterException {
    final String publicKey = "123";
    final HttpServletRequest req = QuickMocker.httpServletRequest(new MockParam(PUBLIC_KEY_PARAMETER, publicKey));

    final Account mockAccount = mock(Account.class);

    when(accountServiceMock.getAccount(eq(Convert.parseHexString(publicKey)))).thenReturn(mockAccount);

    assertEquals(mockAccount, t.getSenderAccount(req));
  }

  @Test(expected = ParameterException.class)
  public void getSenderAccount_withPublicKey_runtimeExceptionGivesParameterException() throws ParameterException {
    final String publicKey = "123";
    final HttpServletRequest req = QuickMocker.httpServletRequest(new MockParam(PUBLIC_KEY_PARAMETER, publicKey));

    when(accountServiceMock.getAccount(eq(Convert.parseHexString(publicKey)))).thenThrow(new RuntimeException());

    t.getSenderAccount(req);
  }

  @Test(expected = ParameterException.class)
  public void getSenderAccount_missingSecretPhraseAndPublicKey() throws ParameterException {
    final HttpServletRequest req = QuickMocker.httpServletRequest();
    t.getSenderAccount(req);
  }

  @Test(expected = ParameterException.class)
  public void getSenderAccount_noAccountFoundResultsInUnknownAccount() throws ParameterException {
    final String publicKey = "123";
    final HttpServletRequest req = QuickMocker.httpServletRequest(new MockParam(PUBLIC_KEY_PARAMETER, publicKey));

    when(accountServiceMock.getAccount(eq(Convert.parseHexString(publicKey)))).thenReturn(null);

    t.getSenderAccount(req);
  }

  @Test
  public void getAliasByAliasId() throws ParameterException {
    final Alias mockAlias = mock(Alias.class);

    final HttpServletRequest req = QuickMocker.httpServletRequest(new MockParam(ALIAS_PARAMETER, "123"));

    when(aliasServiceMock.getAlias(eq(123L))).thenReturn(mockAlias);

    assertEquals(mockAlias, t.getAlias(req));
  }

  @Test
  public void getAliasByAliasName() throws ParameterException {
    final Alias mockAlias = mock(Alias.class);

    final HttpServletRequest req = QuickMocker.httpServletRequest(new MockParam(ALIAS_NAME_PARAMETER, "aliasName"));

    when(aliasServiceMock.getAlias(eq("aliasName"))).thenReturn(mockAlias);

    assertEquals(mockAlias, t.getAlias(req));
  }

  @Test(expected = ParameterException.class)
  public void getAlias_wrongAliasFormatIsIncorrectAlias() throws ParameterException {
    t.getAlias(QuickMocker.httpServletRequest(new MockParam(ALIAS_PARAMETER, "Five")));
  }

  @Test(expected = ParameterException.class)
  public void getAlias_noAliasOrAliasNameGivenIsMissingAliasOrAliasName() throws ParameterException {
    final HttpServletRequest req = QuickMocker.httpServletRequest();
    t.getAlias(req);
  }

  @Test(expected = ParameterException.class)
  public void noAliasFoundIsUnknownAlias() throws ParameterException {
    final Alias mockAlias = mock(Alias.class);

    final HttpServletRequest req = QuickMocker.httpServletRequest(new MockParam(ALIAS_PARAMETER, "123"));

    when(aliasServiceMock.getAlias(eq(123L))).thenReturn(null);

    assertEquals(mockAlias, t.getAlias(req));
  }

  @Test
  public void getAsset() throws ParameterException {
    final HttpServletRequest req = QuickMocker.httpServletRequest(new MockParam(ASSET_PARAMETER, "123"));

    final Asset mockAsset = mock(Asset.class);

    when(assetServiceMock.getAsset(eq(123L))).thenReturn(mockAsset);

    assertEquals(mockAsset, t.getAsset(req));
  }

  @Test(expected = ParameterException.class)
  public void getAsset_missingIdIsMissingAsset() throws ParameterException {
    t.getAsset(QuickMocker.httpServletRequest());
  }

  @Test(expected = ParameterException.class)
  public void getAsset_wrongIdFormatIsIncorrectAsset() throws ParameterException {
    t.getAsset(QuickMocker.httpServletRequest(new MockParam(ASSET_PARAMETER, "twenty")));
  }

  @Test(expected = ParameterException.class)
  public void getAsset_assetNotFoundIsUnknownAsset() throws ParameterException {
    when(assetServiceMock.getAsset(eq(123L))).thenReturn(null);

    t.getAsset(QuickMocker.httpServletRequest(new MockParam(ASSET_PARAMETER, "123")));
  }

  @Test
  public void getGoods() {
    //TODO Write tests after DigitalGoodsStore has been refactored
  }

  //TODO @Brabantian Write further tests
}