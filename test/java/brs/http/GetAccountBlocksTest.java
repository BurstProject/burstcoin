package brs.http;

import static brs.http.common.Parameters.FIRST_INDEX_PARAMETER;
import static brs.http.common.Parameters.LAST_INDEX_PARAMETER;
import static brs.http.common.Parameters.TIMESTAMP_PARAMETER;
import static brs.http.common.ResultFields.BLOCKS_RESPONSE;
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
import brs.services.BlockService;
import brs.services.ParameterService;
import java.util.Arrays;
import javax.servlet.http.HttpServletRequest;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.junit.Before;
import org.junit.Test;
import org.powermock.core.classloader.annotations.SuppressStaticInitializationFor;

@SuppressStaticInitializationFor("brs.Block")
public class GetAccountBlocksTest extends AbstractUnitTest {

  private GetAccountBlocks t;

  private Blockchain blockchainMock;
  private ParameterService parameterServiceMock;
  private BlockService blockServiceMock;

  @Before
  public void setUp() {
    blockchainMock = mock(Blockchain.class);
    parameterServiceMock = mock(ParameterService.class);
    blockServiceMock = mock(BlockService.class);

    t = new GetAccountBlocks(blockchainMock, parameterServiceMock, blockServiceMock);
  }

  @Test
  public void processRequest() throws BurstException {
    final int mockTimestamp = 1;
    final int mockFirstIndex = 2;
    final int mockLastIndex = 3;

    final HttpServletRequest req = QuickMocker.httpServletRequest(
        new MockParam(FIRST_INDEX_PARAMETER, "" + mockFirstIndex),
        new MockParam(LAST_INDEX_PARAMETER, "" + mockLastIndex),
        new MockParam(TIMESTAMP_PARAMETER, "" + mockTimestamp)
    );

    final Account mockAccount = mock(Account.class);
    final Block mockBlock = mock(Block.class);


    when(parameterServiceMock.getAccount(req)).thenReturn(mockAccount);

    final BurstIterator<Block> mockBlockIterator = mockBurstIterator(Arrays.asList(mockBlock));
    when(blockchainMock.getBlocks(eq(mockAccount), eq(mockTimestamp), eq(mockFirstIndex), eq(mockLastIndex))).thenReturn(mockBlockIterator);

    final JSONObject result = (JSONObject) t.processRequest(req);

    final JSONArray blocks = (JSONArray) result.get(BLOCKS_RESPONSE);
    assertNotNull(blocks);
    assertEquals(1, blocks.size());

    final JSONObject resultBlock = (JSONObject) blocks.get(0);
    assertNotNull(resultBlock);

    //TODO validate all fields
  }
}