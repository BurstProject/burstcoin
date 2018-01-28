package brs.http;

import static brs.Constants.MAX_ASSET_DESCRIPTION_LENGTH;
import static brs.Constants.MAX_ASSET_NAME_LENGTH;
import static brs.Constants.MIN_ASSET_NAME_LENGTH;
import static brs.http.JSONResponses.INCORRECT_ASSET_DESCRIPTION;
import static brs.http.JSONResponses.INCORRECT_ASSET_NAME;
import static brs.http.JSONResponses.INCORRECT_ASSET_NAME_LENGTH;
import static brs.http.JSONResponses.INCORRECT_DECIMALS;
import static brs.http.JSONResponses.MISSING_NAME;
import static brs.http.common.Parameters.DECIMALS_PARAMETER;
import static brs.http.common.Parameters.DESCRIPTION_PARAMETER;
import static brs.http.common.Parameters.NAME_PARAMETER;
import static brs.http.common.Parameters.QUANTITY_NQT_PARAMETER;
import static brs.http.common.Parameters.QUANTITY_PARAMETER;
import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;

import brs.Blockchain;
import brs.BurstException;
import brs.TransactionProcessor;
import brs.common.QuickMocker;
import brs.common.QuickMocker.MockParam;
import brs.services.AccountService;
import brs.services.ParameterService;
import javax.servlet.http.HttpServletRequest;
import org.junit.Before;
import org.junit.Test;

public class IssueAssetTest extends AbstractTransactionTest {

  private IssueAsset t;

  private ParameterService mockParameterService;
  private Blockchain mockBlockchain;
  private TransactionProcessor mockTransactionProcessor;
  private AccountService mockAccountService;

  @Before
  public void setUp() {
    mockParameterService = mock(ParameterService.class);
    mockBlockchain = mock(Blockchain.class);
    mockTransactionProcessor = mock(TransactionProcessor.class);
    mockAccountService = mock(AccountService.class);

    t = new IssueAsset(mockParameterService, mockTransactionProcessor, mockBlockchain, mockAccountService);
  }

  @Test
  public void processRequest() throws BurstException {
    final HttpServletRequest req = QuickMocker.httpServletRequest(
        new MockParam(NAME_PARAMETER, stringWithLength(MIN_ASSET_NAME_LENGTH + 1)),
        new MockParam(DESCRIPTION_PARAMETER, stringWithLength(MAX_ASSET_DESCRIPTION_LENGTH - 1)),
        new MockParam(DECIMALS_PARAMETER, 4),
        new MockParam(QUANTITY_NQT_PARAMETER, 5)
    );

    prepareTransactionTest(req, mockParameterService, mockTransactionProcessor);

    t.processRequest(req);
  }

  @Test
  public void processRequest_missingName() throws BurstException {
    final HttpServletRequest req = QuickMocker.httpServletRequest();

    assertEquals(MISSING_NAME, t.processRequest(req));
  }

  @Test
  public void processRequest_incorrectAssetNameLength_smallerThanMin() throws BurstException {
    final HttpServletRequest req = QuickMocker.httpServletRequest(
        new MockParam(NAME_PARAMETER, stringWithLength(MIN_ASSET_NAME_LENGTH - 1))
    );

    assertEquals(INCORRECT_ASSET_NAME_LENGTH, t.processRequest(req));
  }

  @Test
  public void processRequest_incorrectAssetNameLength_largerThanMax() throws BurstException {
    final HttpServletRequest req = QuickMocker.httpServletRequest(
        new MockParam(NAME_PARAMETER, stringWithLength(MAX_ASSET_NAME_LENGTH + 1))
    );

    assertEquals(INCORRECT_ASSET_NAME_LENGTH, t.processRequest(req));
  }

  @Test
  public void processRequest_incorrectAssetName() throws BurstException {
    final HttpServletRequest req = QuickMocker.httpServletRequest(
        new MockParam(NAME_PARAMETER, stringWithLength(MIN_ASSET_NAME_LENGTH + 1) + "[")
    );

    assertEquals(INCORRECT_ASSET_NAME, t.processRequest(req));
  }

  @Test
  public void processRequest_incorrectAssetDescription() throws BurstException {
    final HttpServletRequest req = QuickMocker.httpServletRequest(
        new MockParam(NAME_PARAMETER, stringWithLength(MIN_ASSET_NAME_LENGTH + 1)),
        new MockParam(DESCRIPTION_PARAMETER, stringWithLength(MAX_ASSET_DESCRIPTION_LENGTH + 1))
    );

    assertEquals(INCORRECT_ASSET_DESCRIPTION, t.processRequest(req));
  }

  @Test
  public void processRequest_incorrectDecimals_unParsable() throws BurstException {
    final HttpServletRequest req = QuickMocker.httpServletRequest(
        new MockParam(NAME_PARAMETER, stringWithLength(MIN_ASSET_NAME_LENGTH + 1)),
        new MockParam(DESCRIPTION_PARAMETER, stringWithLength(MAX_ASSET_DESCRIPTION_LENGTH - 1)),
        new MockParam(DECIMALS_PARAMETER, "unParsable")
    );

    assertEquals(INCORRECT_DECIMALS, t.processRequest(req));
  }

  @Test
  public void processRequest_incorrectDecimals_negativeNumber() throws BurstException {
    final HttpServletRequest req = QuickMocker.httpServletRequest(
        new MockParam(NAME_PARAMETER, stringWithLength(MIN_ASSET_NAME_LENGTH + 1)),
        new MockParam(DESCRIPTION_PARAMETER, stringWithLength(MAX_ASSET_DESCRIPTION_LENGTH - 1)),
        new MockParam(DECIMALS_PARAMETER, -5)
    );

    assertEquals(INCORRECT_DECIMALS, t.processRequest(req));
  }

  @Test
  public void processRequest_incorrectDecimals_moreThan8() throws BurstException {
    final HttpServletRequest req = QuickMocker.httpServletRequest(
        new MockParam(NAME_PARAMETER, stringWithLength(MIN_ASSET_NAME_LENGTH + 1)),
        new MockParam(DESCRIPTION_PARAMETER, stringWithLength(MAX_ASSET_DESCRIPTION_LENGTH - 1)),
        new MockParam(DECIMALS_PARAMETER, 9)
    );

    assertEquals(INCORRECT_DECIMALS, t.processRequest(req));
  }

}
