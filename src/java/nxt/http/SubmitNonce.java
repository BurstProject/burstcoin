package nxt.http;

import java.nio.ByteBuffer;

import javax.servlet.http.HttpServletRequest;

import nxt.Block;
import nxt.Generator;
import nxt.Nxt;
import nxt.util.Convert;
import fr.cryptohash.Shabal256;

import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;


public final class SubmitNonce extends APIServlet.APIRequestHandler {
	static final SubmitNonce instance = new SubmitNonce();
	
	private SubmitNonce() {
		super("secretPhrase", "nonce");
	}
	
	@Override
	JSONStreamAware processRequest(HttpServletRequest req) {
		String secret = req.getParameter("secretPhrase");
		Long nonce = Convert.parseUnsignedLong(req.getParameter("nonce"));
		
		JSONObject response = new JSONObject();
		
		if(secret == null) {
			response.put("result", "Missing Passphrase");
			return response;
		}
		
		if(nonce == null) {
			response.put("result", "Missing Nonce");
			return response;
		}
		
		Generator generator = Generator.addNonce(secret, nonce);
		if(generator == null) {
			response.put("result", "failed to create generator");
			return response;
		}
		
		//response.put("result", "deadline: " + generator.getDeadline());
		response.put("result", "success");
		response.put("deadline", generator.getDeadline());
		
		return response;
	}
	
	@Override
    boolean requirePost() {
        return true;
    }
}
