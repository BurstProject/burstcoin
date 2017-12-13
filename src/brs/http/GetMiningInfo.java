package brs.http;

import brs.crypto.hash.Shabal256;
import brs.Block;
import brs.Burst;
import brs.util.Convert;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

import javax.servlet.http.HttpServletRequest;
import java.nio.ByteBuffer;

public final class GetMiningInfo extends APIServlet.APIRequestHandler {
  static final GetMiningInfo instance = new GetMiningInfo();
	
  private GetMiningInfo() {
    super(new APITag[] {APITag.MINING, APITag.INFO});
  }
	
  @Override
  JSONStreamAware processRequest(HttpServletRequest req) {
    JSONObject response = new JSONObject();
		
    response.put("height", Long.toString((long)Burst.getBlockchain().getHeight() + 1));
		
    Block lastBlock = Burst.getBlockchain().getLastBlock();
    byte[] lastGenSig = lastBlock.getGenerationSignature();
    Long lastGenerator = lastBlock.getGeneratorId();
		
    ByteBuffer buf = ByteBuffer.allocate(32 + 8);
    buf.put(lastGenSig);
    buf.putLong(lastGenerator);
		
    Shabal256 md = new Shabal256();
    md.update(buf.array());
    byte[] newGenSig = md.digest();
		
    response.put("generationSignature", Convert.toHexString(newGenSig));
    response.put("baseTarget", Long.toString(lastBlock.getBaseTarget()));
		
    return response;
  }
}
