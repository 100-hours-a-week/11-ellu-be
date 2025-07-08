package com.ellu.looper.commons.util;

import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class CacheService {

  private final RedisTemplate<String, Object> redisTemplate;
  private final RedissonClient redissonClient;

  public static long addJitter(long baseTtl, double jitterPercent) {
    long jitter = (long) (baseTtl * jitterPercent);
    // Get the current thread's dedicated random number generator
    return baseTtl + ThreadLocalRandom.current().nextLong(0, jitter + 1);
  }

  /**
   * Gets a value from cache with locking pattern. If cache miss occurs, the first request locks the
   * key and updates the cache. Other requests wait until the cache is updated.
   */
  public <T> T getWithLock(String cacheKey, long ttlSeconds, Supplier<T> dataSupplier) {
    return getWithLock(cacheKey, ttlSeconds, dataSupplier, 0.001); // Default jitter
  }

  /**
   * Gets a value from cache with locking pattern and custom jitter.
   *
   * @param <T> the type of cached data
   * @return the cached data
   */
  public <T> T getWithLock(
      String cacheKey, long ttlSeconds, Supplier<T> dataSupplier, double jitterPercent) {
    // Cache hit -> get from cache
    T cached = (T) redisTemplate.opsForValue().get(cacheKey);
    if (cached != null) {
      log.info("Cache hit for project detail: {}", cacheKey);
      return cached;
    }

    log.info("Cache miss for key: {}. Attempting to acquire lock.", cacheKey);

    // Cache miss -> try to acquire lock
    String lockKey = "lock:" + cacheKey;
    RLock lock = redissonClient.getLock(lockKey);

    try {
      // Try to acquire lock
      if (lock.tryLock(1, 10, TimeUnit.SECONDS)) { // Wait 1s, lease 10s
        try {
          // Double check cache after acquiring lock
          cached = (T) redisTemplate.opsForValue().get(cacheKey);
          if (cached != null) {
            log.debug("Cache populated by another thread for key: {}", cacheKey);
            return cached;
          }

          // Load data from db and cache it
          log.debug("Loading data from database for key: {}", cacheKey);
          T data = dataSupplier.get();

          if (data != null) {
            setProjectCache(cacheKey, data, ttlSeconds);
          }

          return data;
        } finally {
          lock.unlock();
          log.debug("Lock released for key: {}", cacheKey);
        }
      } else {
        // Lock acquisition failed, another thread is likely updating the cache.
        // Wait for the cache to be populated by the thread that acquired the lock.
        log.debug("Lock acquisition failed for key: {}. Waiting for cache update.", cacheKey);
        return waitForCacheUpdate(cacheKey, dataSupplier);
      }
    } catch (InterruptedException e) {
      log.error("Interrupted while acquiring lock for key: {}", cacheKey, e);
      Thread.currentThread().interrupt();
      // In case of interruption, consider falling back to dataSupplier if critical
      return dataSupplier.get();
    }
  }

  /**
   * Waits for cache to be updated by another thread.
   *
   * @param dataSupplier fallback supplier if waiting fails
   * @param <T> the type of cached data
   * @return the cached data or fallback data
   */
  private <T> T waitForCacheUpdate(String cacheKey, Supplier<T> dataSupplier) {
    int maxWaitAttempts = 10; // 1 second total wait time
    long waitIntervalMs = 100; // 100ms between checks

    for (int i = 0; i < maxWaitAttempts; i++) {
      try {
        Thread.sleep(waitIntervalMs);
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
        log.warn("Thread interrupted while waiting for cache update: {}", cacheKey);
        break;
      }

      T cached = (T) redisTemplate.opsForValue().get(cacheKey);
      if (cached != null) {
        log.debug("Cache updated by another thread for key: {} after {} attempts", cacheKey, i + 1);
        return cached;
      }
    }

    log.warn("Cache not updated within timeout for key: {}. Using fallback data.", cacheKey);
    return dataSupplier.get();
  }

  // Invalidates cache entry and releases any associated locks
  public void invalidate(String cacheKey) {
    redisTemplate.delete(cacheKey);
    String lockKey = "lock:" + cacheKey;
    redisTemplate.delete(lockKey);
    log.debug("Cache invalidated for key: {}", cacheKey);
  }

  // Sets a value in cache with TTL and jitter
  public void set(String cacheKey, Object value, long ttlSeconds, double jitterPercent) {
    long ttlWithJitter = addJitter(ttlSeconds, jitterPercent);
    redisTemplate.opsForValue().set(cacheKey, value, ttlWithJitter, TimeUnit.SECONDS);
    log.debug("Data cached for key: {} with TTL: {} seconds", cacheKey, ttlWithJitter);
  }

  // Sets a value in cache with TTL and default jitter
  public void setNotificationCache(String cacheKey, Object value, long ttlSeconds) {
    set(cacheKey, value, ttlSeconds, 0.1);
  }

  public void setProjectCache(String cacheKey, Object value, long ttlSeconds) {
    set(cacheKey, value, ttlSeconds, 0.001);
  }
}
