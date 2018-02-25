package brs.db;

import brs.Account;
import brs.db.sql.DbKey;
import org.ehcache.Cache;
import org.ehcache.CacheManager;
import org.ehcache.config.builders.CacheConfigurationBuilder;
import org.ehcache.config.builders.CacheManagerBuilder;
import org.ehcache.config.builders.ResourcePoolsBuilder;

public class DBCacheManagerImpl {

  private final CacheManager cacheManager;

  public DBCacheManagerImpl() {
    cacheManager = CacheManagerBuilder.newCacheManagerBuilder().withCache(
        "account", CacheConfigurationBuilder.newCacheConfigurationBuilder(DbKey.class, Account.class, ResourcePoolsBuilder.heap(100)).build()
    ).build(true);
  }

  public void close() {
    cacheManager.close();
  }

  public Cache getCache(String name) {
    switch (name) {
      case "account":
        return cacheManager.getCache(name, DbKey.class, Account.class);
      default:
        return null;
    }
  }

  public void flushCache() {
    getCache("account").clear();
  }

}
