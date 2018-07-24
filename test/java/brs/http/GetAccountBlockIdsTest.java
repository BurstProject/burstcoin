package brs.http;

import static brs.http.common.Parameters.FIRST_INDEX_PARAMETER;
import static brs.http.common.Parameters.LAST_INDEX_PARAMETER;
import static brs.http.common.Parameters.TIMESTAMP_PARAMETER;
import static brs.http.common.ResultFields.BLOCK_IDS_RESPONSE;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import brs.Account;
import brs.Block;
import brs.Blockchain;
import brs.BurstException;
import brs.common.AbstractUnitTest;
import brs.common.QuickMocker;
import brs.common.QuickMocker.MockParam;
import brs.db.BurstIterator;
import brs.services.ParameterService;
import javax.servlet.http.HttpServletRequest;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.junit.Before;
import org.junit.Test;

public class GetAccountBlockIdsTest extends AbstractUnitTest {

  private GetAccountBlockIds t;

  private ParameterService mockParameterService;
  private Blockchain mockBlockchain;

  @Before
  public void setUp() {
    mockParameterService = mock(ParameterService.class);
    mockBlockchain = mock(Blockchain.class);

    t = new GetAccountBlockIds(mockParameterService, mockBlockchain);
  }

  @Test
  public void processRequest() throws BurstException {
    final int timestamp = 1;
    final int firstIndex = 0;
    final int lastIndex = 1;

    final HttpServletRequest req = QuickMocker.httpServletRequest(
        new MockParam(TIMESTAMP_PARAMETER, timestamp),
        new MockParam(FIRST_INDEX_PARAMETER, firstIndex),
        new MockParam(LAST_INDEX_PARAMETER, lastIndex)
    );

    final Account mockAccount = mock(Account.class);

    String mockBlockStringId = "mockBlockStringId";
    final Block mockBlock = mock(Block.class);
    when(mockBlock.getStringId()).thenReturn(mockBlockStringId);
    final BurstIterator<Block> mockBlocksIterator = mockBurstIterator(mockBlock);

    when(mockParameterService.getAccount(req)).thenReturn(mockAccount);
    when(mockBlockchain.getBlocks(eq(mockAccount), eq(timestamp), eq(firstIndex), eq(lastIndex)))
        .thenReturn(mockBlocksIterator);

    final JSONObject result = (JSONObject) t.processRequest(req);
    assertNotNull(result);

    final JSONArray blockIds = (JSONArray) result.get(BLOCK_IDS_RESPONSE);
    assertNotNull(blockIds);
    assertEquals(1, blockIds.size());
    assertEquals(mockBlockStringId, blockIds.get(0));
  }
}