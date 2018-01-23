package brs.http;

import static brs.http.JSONResponses.INCORRECT_DGS_LISTING_DESCRIPTION;
import static brs.http.JSONResponses.INCORRECT_DGS_LISTING_NAME;
import static brs.http.JSONResponses.INCORRECT_DGS_LISTING_TAGS;
import static brs.http.JSONResponses.MISSING_NAME;
import static brs.http.common.Parameters.DESCRIPTION_PARAMETER;
import static brs.http.common.Parameters.NAME_PARAMETER;
import static brs.http.common.Parameters.PRICE_NQT_PARAMETER;
import static brs.http.common.Parameters.QUANTITY_PARAMETER;
import static brs.http.common.Parameters.TAGS_PARAMETER;
import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import brs.Account;
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

public class DGSListingTest extends AbstractTransactionTest {

  private DGSListing t;

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

    t = new DGSListing(mockParameterService, mockTransactionProcessor, mockBlockchain, mockAccountService);
  }

  @Test
  public void processRequest() throws BurstException {
    final Account mockAccount = mock(Account.class);

    final HttpServletRequest req = QuickMocker.httpServletRequest(
        new MockParam(PRICE_NQT_PARAMETER, 123),
        new MockParam(QUANTITY_PARAMETER, 1),
        new MockParam(NAME_PARAMETER, "name"),
        new MockParam(DESCRIPTION_PARAMETER, "description"),
        new MockParam(TAGS_PARAMETER, "tags")
    );

    when(mockParameterService.getSenderAccount(eq(req))).thenReturn(mockAccount);

    prepareTransactionTest(req, mockParameterService, mockTransactionProcessor, mockAccount);

    t.processRequest(req);
  }

  @Test
  public void processRequest_missingName() throws BurstException {
    final HttpServletRequest req = QuickMocker.httpServletRequest(
        new MockParam(PRICE_NQT_PARAMETER, 123),
        new MockParam(QUANTITY_PARAMETER, 1)
    );

    assertEquals(MISSING_NAME, t.processRequest(req));
  }

  @Test
  public void processRequest_incorrectDGSListingName() throws BurstException {
    String tooLongName = "";

    for (int i = 0; i < 101; i++) {
      tooLongName += "a";
    }

    final HttpServletRequest req = QuickMocker.httpServletRequest(
        new MockParam(PRICE_NQT_PARAMETER, 123),
        new MockParam(QUANTITY_PARAMETER, 1),
        new MockParam(NAME_PARAMETER, tooLongName)
    );

    assertEquals(INCORRECT_DGS_LISTING_NAME, t.processRequest(req));
  }

  @Test
  public void processRequest_incorrectDgsListingDescription() throws BurstException {
    String tooLongDescription = "";

    for (int i = 0; i < 1001; i++) {
      tooLongDescription += "a";
    }

    final HttpServletRequest req = QuickMocker.httpServletRequest(
        new MockParam(PRICE_NQT_PARAMETER, 123),
        new MockParam(QUANTITY_PARAMETER, 1),
        new MockParam(NAME_PARAMETER, "name"),
        new MockParam(DESCRIPTION_PARAMETER, tooLongDescription)
    );

    assertEquals(INCORRECT_DGS_LISTING_DESCRIPTION, t.processRequest(req));
  }

  @Test
  public void processRequest_incorrectDgsListingTags() throws BurstException {
    String tooLongTags = "";

    for (int i = 0; i < 101; i++) {
      tooLongTags += "a";
    }

    final HttpServletRequest req = QuickMocker.httpServletRequest(
        new MockParam(PRICE_NQT_PARAMETER, 123),
        new MockParam(QUANTITY_PARAMETER, 1),
        new MockParam(NAME_PARAMETER, "name"),
        new MockParam(DESCRIPTION_PARAMETER, "description"),
        new MockParam(TAGS_PARAMETER, tooLongTags)
    );

    assertEquals(INCORRECT_DGS_LISTING_TAGS, t.processRequest(req));
  }

}
