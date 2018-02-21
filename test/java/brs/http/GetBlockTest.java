package brs.http;

import static brs.http.JSONResponses.INCORRECT_BLOCK;
import static brs.http.JSONResponses.INCORRECT_HEIGHT;
import static brs.http.JSONResponses.INCORRECT_TIMESTAMP;
import static brs.http.JSONResponses.UNKNOWN_BLOCK;
import static brs.http.common.Parameters.BLOCK_PARAMETER;
import static brs.http.common.Parameters.HEIGHT_PARAMETER;
import static brs.http.common.Parameters.TIMESTAMP_PARAMETER;
import static org.junit.Assert.*;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import brs.Block;
import brs.Blockchain;
import brs.common.QuickMocker;
import brs.common.QuickMocker.MockParam;
import brs.services.BlockService;
import javax.servlet.http.HttpServletRequest;
import org.json.simple.JSONObject;
import org.junit.Before;
import org.junit.Test;

public class GetBlockTest {

  private GetBlock t;

  private Blockchain blockchainMock;
  private BlockService blockServiceMock;

  @Before
  public void setUp() {
    blockchainMock = mock(Blockchain.class);
    blockServiceMock = mock(BlockService.class);

    t = new GetBlock(blockchainMock, blockServiceMock);
  }

  @Test
  public void processRequest_withBlockId() {
    long blockId = 2L;

    final HttpServletRequest req = QuickMocker.httpServletRequest(
        new MockParam(BLOCK_PARAMETER, blockId)
    );

    final Block mockBlock = mock(Block.class);

    when(blockchainMock.getBlock(eq(blockId))).thenReturn(mockBlock);

    final JSONObject result = (JSONObject) t.processRequest(req);

    assertNotNull(result);
  }

  @Test
  public void processRequest_withBlockId_incorrectBlock() {
    final HttpServletRequest req = QuickMocker.httpServletRequest(
      new MockParam(BLOCK_PARAMETER, "notALong")
    );

    assertEquals(INCORRECT_BLOCK, t.processRequest(req));
  }

  @Test
  public void processRequest_withHeight() {
    int blockHeight = 2;

    final HttpServletRequest req = QuickMocker.httpServletRequest(
        new MockParam(HEIGHT_PARAMETER, blockHeight)
    );

    final Block mockBlock = mock(Block.class);

    when(blockchainMock.getHeight()).thenReturn(100);
    when(blockchainMock.getBlockAtHeight(eq(blockHeight))).thenReturn(mockBlock);

    final JSONObject result = (JSONObject) t.processRequest(req);

    assertNotNull(result);
  }

  @Test
  public void processRequest_withHeight_incorrectHeight_unParsable() {
    final HttpServletRequest req = QuickMocker.httpServletRequest(
        new MockParam(HEIGHT_PARAMETER, "unParsable")
    );

    assertEquals(INCORRECT_HEIGHT, t.processRequest(req));
  }

  @Test
  public void processRequest_withHeight_incorrectHeight_isNegative() {
    final long heightValue = -1L;

    final HttpServletRequest req = QuickMocker.httpServletRequest(
        new MockParam(HEIGHT_PARAMETER, heightValue)
    );

    assertEquals(INCORRECT_HEIGHT, t.processRequest(req));
  }

  @Test
  public void processRequest_withHeight_incorrectHeight_overCurrentBlockHeight() {
    final long heightValue = 10L;

    final HttpServletRequest req = QuickMocker.httpServletRequest(
        new MockParam(HEIGHT_PARAMETER, heightValue)
    );

    when(blockchainMock.getHeight()).thenReturn(5);

    assertEquals(INCORRECT_HEIGHT, t.processRequest(req));
  }

  @Test
  public void processRequest_withTimestamp() {
    int timestamp = 2;

    final HttpServletRequest req = QuickMocker.httpServletRequest(
        new MockParam(TIMESTAMP_PARAMETER, timestamp)
    );

    final Block mockBlock = mock(Block.class);

    when(blockchainMock.getLastBlock(eq(timestamp))).thenReturn(mockBlock);

    final JSONObject result = (JSONObject) t.processRequest(req);

    assertNotNull(result);
  }

  @Test
  public void processRequest_withTimestamp_incorrectTimeStamp_unParsable() {
    final HttpServletRequest req = QuickMocker.httpServletRequest(
      new MockParam(TIMESTAMP_PARAMETER, "unParsable")
    );

    assertEquals(INCORRECT_TIMESTAMP, t.processRequest(req));
  }

  @Test
  public void processRequest_withTimestamp_incorrectTimeStamp_negative() {
    final int timestamp = -1;

    final HttpServletRequest req = QuickMocker.httpServletRequest(
        new MockParam(TIMESTAMP_PARAMETER, timestamp)
    );

    assertEquals(INCORRECT_TIMESTAMP, t.processRequest(req));
  }


  @Test
  public void processRequest_unknownBlock() {
    final HttpServletRequest req = QuickMocker.httpServletRequest();

    assertEquals(UNKNOWN_BLOCK, t.processRequest(req));
  }

}
