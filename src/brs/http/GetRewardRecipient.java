package brs.http;

import brs.Account;
import brs.Burst;
import brs.BurstException;
import brs.services.ParameterService;
import brs.util.Convert;
import javax.servlet.http.HttpServletRequest;
import org.json.simple.JSONObject;
import org.json.simple.JSONStreamAware;

public final class GetRewardRecipient extends APIServlet.APIRequestHandler {

  private final ParameterService parameterService;

  GetRewardRecipient(ParameterService parameterService) {
    super(new APITag[]{APITag.ACCOUNTS, APITag.MINING, APITag.INFO}, "account");
    this.parameterService = parameterService;
  }

  @Override
  JSONStreamAware processRequest(HttpServletRequest req) throws BurstException {
    JSONObject response = new JSONObject();

    final Account account = parameterService.getAccount(req);
    Account.RewardRecipientAssignment assignment = account.getRewardRecipientAssignment();
    long height = Burst.getBlockchain().getLastBlock().getHeight();
    if (account == null || assignment == null) {
      response.put("rewardRecipient", Convert.toUnsignedLong(account.getId()));
    } else if (assignment.getFromHeight() > height + 1) {
      response.put("rewardRecipient", Convert.toUnsignedLong(assignment.getPrevRecipientId()));
    } else {
      response.put("rewardRecipient", Convert.toUnsignedLong(assignment.getRecipientId()));
    }

    return response;
  }

}
