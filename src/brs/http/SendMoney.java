package brs.http;

import brs.Account;
import brs.BurstException;
import org.json.simple.JSONStreamAware;

import javax.servlet.http.HttpServletRequest;

public final class SendMoney extends CreateTransaction {

  static final SendMoney instance = new SendMoney();

  private SendMoney() {
    super(new APITag[] {APITag.ACCOUNTS, APITag.CREATE_TRANSACTION}, "recipient", "amountNQT");
  }

  @Override
  JSONStreamAware processRequest(HttpServletRequest req) throws BurstException {
    long recipient = ParameterParser.getRecipientId(req);
    long amountNQT = ParameterParser.getAmountNQT(req);
    Account account = ParameterParser.getSenderAccount(req);
    return createTransaction(req, account, recipient, amountNQT);
  }

}
