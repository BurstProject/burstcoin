package nxt.http;

import java.nio.ByteBuffer;

import javax.servlet.http.HttpServletRequest;

import nxt.Account;
import nxt.Block;
import nxt.Generator;
import nxt.Nxt;
import nxt.crypto.Crypto;
import nxt.util.Convert;
import fr.cryptohash.Shabal256;

import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;


public final class SubmitNonce extends APIServlet.APIRequestHandler {
	static final SubmitNonce instance = new SubmitNonce();
	
	private SubmitNonce() {
		super(new APITag[] {APITag.MINING}, "secretPhrase", "nonce", "accountId");
	}
	
	@Override
	JSONStreamAware processRequest(HttpServletRequest req) {
		String secret = req.getParameter("secretPhrase");
		Long nonce = Convert.parseUnsignedLong(req.getParameter("nonce"));
		
		String accountId = req.getParameter("accountId");
		
		JSONObject response = new JSONObject();
		
		if(secret == null) {
			response.put("result", "Missing Passphrase");
			return response;
		}
		
		if(nonce == null) {
			response.put("result", "Missing Nonce");
			return response;
		}
		
		byte[] secretPublicKey = Crypto.getPublicKey(secret);
		Account secretAccount = Account.getAccount(secretPublicKey);
		if(secretAccount != null) {
			Account genAccount;
			if(accountId != null) {
				genAccount = Account.getAccount(Convert.parseAccountId(accountId));
			}
			else {
				genAccount = secretAccount;
			}
			
			if(genAccount != null) {
				Account.RewardRecipientAssignment assignment = genAccount.getRewardRecipientAssignment();
				Long rewardId;
				if(assignment == null) {
					rewardId = genAccount.getId();
				}
				else if(assignment.getFromHeight() > Nxt.getBlockchain().getLastBlock().getHeight() + 1) {
					rewardId = assignment.getPrevRecipientId();
				}
				else {
					rewardId = assignment.getRecipientId();
				}
				if(rewardId != secretAccount.getId()) {
					response.put("result", "Passphrase does not match reward recipient");
					return response;
				}
			}
			else {
				response.put("result", "Passphrase is for a different account");
				return response;
			}
		}
		
		Generator.GeneratorState generator;
		if(accountId == null || secretAccount == null) {
			generator = Nxt.getGenerator().addNonce(secret, nonce);
		}
		else {
			Account genAccount = Account.getAccount(Convert.parseUnsignedLong(accountId));
			if(genAccount == null ||
			   genAccount.getPublicKey() == null) {
				response.put("result", "Passthrough mining requires public key in blockchain");
			}
			byte[] publicKey = genAccount.getPublicKey();
			generator = Nxt.getGenerator().addNonce(secret, nonce, publicKey);
		}
		
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
