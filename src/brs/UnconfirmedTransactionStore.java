package brs;

import brs.db.cache.TransactionExpiry;
import java.util.ArrayList;
import java.util.Collection;
import java.util.function.Consumer;
import org.ehcache.Cache;
import org.ehcache.CacheManager;
import org.ehcache.config.CacheConfiguration;
import org.ehcache.config.builders.CacheConfigurationBuilder;
import org.ehcache.config.builders.CacheManagerBuilder;
import org.ehcache.config.builders.ResourcePoolsBuilder;


public class UnconfirmedTransactionStore {
  private final static String cacheName = "unconfirmedTransactionStore";
  private final CacheManager cacheManager;
  private final CacheConfiguration cacheConfiguration;

  public UnconfirmedTransactionStore() {
    cacheConfiguration = CacheConfigurationBuilder.newCacheConfigurationBuilder(
        Long.class, Transaction.class,
        ResourcePoolsBuilder.heap(8192)
    ).withExpiry(
        new TransactionExpiry()
    ).build();
    cacheManager = CacheManagerBuilder.newCacheManagerBuilder().withCache(
        cacheName, cacheConfiguration
    ).build(true);
  }

  public void close() {
    cacheManager.close();
  }

  public Cache getCache() {
    return cacheManager.getCache(cacheName, cacheConfiguration.getKeyType(), cacheConfiguration.getValueType());
  }

  public void put(Collection<Transaction> transactions) {
    for ( Transaction transaction : transactions ) {
      put(transaction);
    }
  }

  public void put(Transaction transaction) {
    getCache().put(transaction.getId(), transaction);
  }

  public Transaction get(Long transactionId) {
    return (Transaction) getCache().get(transactionId);
  }

  public boolean exists(Long transactionId) {
    return getCache().containsKey(transactionId);
  }

  public ArrayList<Transaction> getAll() {
    ArrayList<Transaction> transactions = new ArrayList<>();
    getCache().forEach( e -> {
      Transaction transaction = (Transaction) ((Cache.Entry) e).getValue();
      transactions.add(transaction);
    });
    return transactions;
  }

  public void forEach(Consumer<Transaction> consumer) {
    getCache().forEach( e -> {
      Transaction transaction = (Transaction) ((Cache.Entry) e).getValue();
      consumer.accept(transaction);
    } );
  }

  public void remove(Transaction transaction) {
    getCache().remove(transaction.getId());
  }

  public void clear() {
    getCache().clear();
  }
}
