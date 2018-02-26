package brs.db.cache;

import brs.Account;
import brs.db.sql.DbKey;
import brs.statistics.StatisticsManagerImpl;
import org.ehcache.Cache;
import org.ehcache.CacheManager;
import org.ehcache.config.builders.CacheConfigurationBuilder;
import org.ehcache.config.builders.CacheManagerBuilder;
import org.ehcache.config.builders.ResourcePoolsBuilder;

public class DBCacheManagerImpl {

  private final CacheManager cacheManager;

  private final StatisticsManagerImpl statisticsManager;

  private final boolean statisticsEnabled;

  public DBCacheManagerImpl(StatisticsManagerImpl statisticsManager) {
    this.statisticsManager = statisticsManager;
    statisticsEnabled = true;

    cacheManager = CacheManagerBuilder.newCacheManagerBuilder()
        .withCache("account", CacheConfigurationBuilder.newCacheConfigurationBuilder(DbKey.class, Account.class, ResourcePoolsBuilder.heap(100)).build()
        ).build(true);
  }

  public void close() {
    cacheManager.close();
  }

  private Cache getEHCache(String name) {
    switch (name) {
      case "account":
        return cacheManager.getCache(name, DbKey.class, Account.class);
      default:
        return null;
    }
  }

  public Cache getCache(String name) {
    Cache cache = getEHCache(name);
    return statisticsEnabled ? new StatisticsCache(cache, name, statisticsManager) : cache;
  }

  public void flushCache() {
    getEHCache("account").clear();
  }

}
