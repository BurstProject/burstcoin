package brs.db.cache;

import brs.Transaction;
import brs.util.Time;
import org.ehcache.expiry.ExpiryPolicy;
import java.time.Duration;
import java.util.function.Supplier;

public class TransactionExpiry implements ExpiryPolicy<Long, Transaction> {

  private static volatile Time time = new Time.EpochTime();

  @Override
  public Duration getExpiryForCreation(Long key, Transaction value) {
    return Duration.ofSeconds((long) value.getExpiration() - time.getTime());
  }

  @Override
  public Duration getExpiryForAccess(Long key, Supplier<? extends Transaction> value) {
    return Duration.ofSeconds((long) value.get().getExpiration() - time.getTime());
  }

  @Override
  public Duration getExpiryForUpdate(Long key, Supplier<? extends Transaction> oldValue, Transaction newValue) {
    return Duration.ofSeconds((long) newValue.getExpiration() - time.getTime());
  }
}
