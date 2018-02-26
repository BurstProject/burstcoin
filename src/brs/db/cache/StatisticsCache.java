package brs.db.cache;

import brs.statistics.StatisticsManagerImpl;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import org.ehcache.Cache;
import org.ehcache.config.CacheRuntimeConfiguration;
import org.ehcache.spi.loaderwriter.BulkCacheLoadingException;
import org.ehcache.spi.loaderwriter.BulkCacheWritingException;
import org.ehcache.spi.loaderwriter.CacheLoadingException;
import org.ehcache.spi.loaderwriter.CacheWritingException;

public class StatisticsCache<K, V> implements Cache<K, V> {

  private final Cache<K, V> wrappedCache;
  private final StatisticsManagerImpl statisticsManager;
  private final String cacheName;

  public StatisticsCache(Cache<K, V> wrappedCache, String cacheName, StatisticsManagerImpl statisticsManager) {
    this.wrappedCache = wrappedCache;
    this.statisticsManager = statisticsManager;
    this.cacheName = cacheName;
  }

  @Override
  public V get(K k) throws CacheLoadingException {
    final V result = wrappedCache.get(k);
    return result;
  }

  @Override
  public void put(K k, V v) throws CacheWritingException {
    wrappedCache.put(k, v);
  }

  @Override
  public boolean containsKey(K k) {
    final boolean result = wrappedCache.containsKey(k);

    if(result) {
      statisticsManager.foundObjectInCache(cacheName);
    } else {
      statisticsManager.didNotFindObjectInCache(cacheName);
    }

    return result;
  }

  @Override
  public void remove(K k) throws CacheWritingException {
    wrappedCache.remove(k);
  }

  @Override
  public Map<K, V> getAll(Set<? extends K> set) throws BulkCacheLoadingException {
    return wrappedCache.getAll(set);
  }

  @Override
  public void putAll(Map<? extends K, ? extends V> map) throws BulkCacheWritingException {
    wrappedCache.putAll(map);
  }

  @Override
  public void removeAll(Set<? extends K> set) throws BulkCacheWritingException {
    wrappedCache.removeAll(set);
  }

  @Override
  public void clear() {
    wrappedCache.clear();
  }

  @Override
  public V putIfAbsent(K k, V v) throws CacheLoadingException, CacheWritingException {
    return wrappedCache.putIfAbsent(k, v);
  }

  @Override
  public boolean remove(K k, V v) throws CacheWritingException {
    return wrappedCache.remove(k, v);
  }

  @Override
  public V replace(K k, V v) throws CacheLoadingException, CacheWritingException {
    return wrappedCache.replace(k, v);
  }

  @Override
  public boolean replace(K k, V v, V v1) throws CacheLoadingException, CacheWritingException {
    return wrappedCache.replace(k, v, v1);
  }

  @Override
  public CacheRuntimeConfiguration<K, V> getRuntimeConfiguration() {
    return wrappedCache.getRuntimeConfiguration();
  }

  @Override
  public Iterator<Entry<K, V>> iterator() {
    return wrappedCache.iterator();
  }
}
