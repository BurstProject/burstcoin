package brs.unconfirmedtransactions;

import brs.Account;
import brs.BurstException;
import brs.Transaction;
import brs.db.store.AccountStore;
import brs.util.Convert;
import java.util.HashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class ReservedBalanceCache {

  private static final Logger LOGGER = LoggerFactory.getLogger(ReservedBalanceCache.class);

  private final AccountStore accountStore;

  private final HashMap<Long, Long> reservedBalanceCache;

  public ReservedBalanceCache(AccountStore accountStore) {
    this.accountStore = accountStore;

    this.reservedBalanceCache = new HashMap<>();
  }

  void reserveBalanceAndPut(Transaction transaction) throws BurstException.ValidationException {
    Account senderAccount = null;

    if(transaction.getSenderId() != 0) {
      senderAccount = accountStore.getAccountTable().get(accountStore.getAccountKeyFactory().newKey(transaction.getSenderId()));
    }

    final Long amountNQT = Convert.safeAdd(
        reservedBalanceCache.getOrDefault(transaction.getSenderId(), 0L),
        transaction.getType().calculateTotalAmountNQT(transaction)
    );

    if (senderAccount == null) {
      LOGGER.debug(String.format("Account %d does not exist and has no balance. Required funds: %d",transaction.getSenderId(), amountNQT));

      throw new BurstException.NotCurrentlyValidException("Account unknown");
    } else if ( amountNQT > senderAccount.getUnconfirmedBalanceNQT() ) {
      LOGGER.debug(String.format("Account %d balance to low. You have  %d > %d Balance",
          transaction.getSenderId(), amountNQT, senderAccount.getUnconfirmedBalanceNQT()
      ));

      throw new BurstException.NotCurrentlyValidException("Insufficient funds");
    }

    reservedBalanceCache.put(transaction.getSenderId(), amountNQT);
  }

  void refundBalance(Transaction transaction) {
    Long amountNQT = Convert.safeSubtract(
        reservedBalanceCache.getOrDefault(transaction.getSenderId(), 0L),
        transaction.getType().calculateTotalAmountNQT(transaction)
    );

    if (amountNQT > 0) {
      reservedBalanceCache.put(transaction.getSenderId(), amountNQT);
    } else {
      reservedBalanceCache.remove(transaction.getSenderId());
    }
  }

  public void clear() {
    reservedBalanceCache.clear();
  }
}
