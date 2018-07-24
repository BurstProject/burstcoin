package brs.peer;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import brs.Block;
import brs.Blockchain;
import brs.common.QuickMocker;
import java.math.BigInteger;
import org.json.simple.JSONObject;
import org.junit.Before;
import org.junit.Test;

public class GetCumulativeDifficultyTest {

  private GetCumulativeDifficulty t;

  private Blockchain mockBlockchain;

  @Before
  public void setUp() {
    mockBlockchain = mock(Blockchain.class);

    t = new GetCumulativeDifficulty(mockBlockchain);
  }

  @Test
  public void processRequest() {
    final BigInteger cumulativeDifficulty = BigInteger.TEN;
    final int blockchainHeight = 50;

    final JSONObject request = QuickMocker.jsonObject();

    final Block mockLastBlock = mock(Block.class);
    when(mockLastBlock.getHeight()).thenReturn(blockchainHeight);
    when(mockLastBlock.getCumulativeDifficulty()).thenReturn(cumulativeDifficulty);

    when(mockBlockchain.getLastBlock()).thenReturn(mockLastBlock);

    final JSONObject result = (JSONObject) t.processRequest(request, mock(Peer.class));
    assertNotNull(result);

    assertEquals(cumulativeDifficulty.toString(), result.get("cumulativeDifficulty"));
    assertEquals(blockchainHeight, result.get("blockchainHeight"));
  }

}
