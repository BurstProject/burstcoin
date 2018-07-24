package brs.http;

import static brs.fluxcapacitor.FeatureToggle.DIGITAL_GOODS_STORE;
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
import static org.junit.Assert.assertNotNull;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.powermock.api.mockito.PowerMockito.mockStatic;

import brs.Account;
import brs.Attachment;
import brs.Blockchain;
import brs.Burst;
import brs.BurstException;
import brs.TransactionType.DigitalGoods;
import brs.common.QuickMocker;
import brs.common.QuickMocker.MockParam;
import brs.fluxcapacitor.FluxCapacitor;
import brs.services.ParameterService;
import javax.servlet.http.HttpServletRequest;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

@RunWith(PowerMockRunner.class)
@PrepareForTest(Burst.class)
public class DGSListingTest extends AbstractTransactionTest {

  private DGSListing t;

  private ParameterService mockParameterService;
  private Blockchain mockBlockchain;
  private APITransactionManager apiTransactionManagerMock;

  @Before
  public void setUp() {
    mockParameterService = mock(ParameterService.class);
    mockBlockchain = mock(Blockchain.class);
    apiTransactionManagerMock = mock(APITransactionManager.class);

    t = new DGSListing(mockParameterService, mockBlockchain, apiTransactionManagerMock);
  }

  @Test
  public void processRequest() throws BurstException {
    final Account mockAccount = mock(Account.class);

    final String dgsName = "dgsName";
    final String dgsDescription = "dgsDescription";
    final String tags = "tags";
    final int priceNqt = 123;
    final int quantity = 5;

    final HttpServletRequest req = QuickMocker.httpServletRequest(
        new MockParam(PRICE_NQT_PARAMETER, priceNqt),
        new MockParam(QUANTITY_PARAMETER, quantity),
        new MockParam(NAME_PARAMETER, dgsName),
        new MockParam(DESCRIPTION_PARAMETER, dgsDescription),
        new MockParam(TAGS_PARAMETER, tags)
    );

    when(mockParameterService.getSenderAccount(eq(req))).thenReturn(mockAccount);

    mockStatic(Burst.class);
    final FluxCapacitor fluxCapacitor = QuickMocker.fluxCapacitorEnabledFunctionalities(DIGITAL_GOODS_STORE);
    when(Burst.getFluxCapacitor()).thenReturn(fluxCapacitor);

    final Attachment.DigitalGoodsListing attachment = (Attachment.DigitalGoodsListing) attachmentCreatedTransaction(() -> t.processRequest(req), apiTransactionManagerMock);
    assertNotNull(attachment);

    assertEquals(DigitalGoods.LISTING, attachment.getTransactionType());
    assertEquals(dgsName, attachment.getName());
    assertEquals(dgsDescription, attachment.getDescription());
    assertEquals(tags, attachment.getTags());
    assertEquals(priceNqt, attachment.getPriceNQT());
    assertEquals(quantity, attachment.getQuantity());
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
