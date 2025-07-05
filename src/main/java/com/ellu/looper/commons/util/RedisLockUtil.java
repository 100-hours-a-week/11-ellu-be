package com.ellu.looper.commons.util;

import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class RedisLockUtil {

  private final RedisTemplate<String, Object> redisTemplate;

  // Lock TTL in seconds - should be longer than expected DB operation time
  private static final long LOCK_TTL_SECONDS = 5L;

  // Wait time between retries in milliseconds
  private static final long RETRY_WAIT_MS = 100L;

  // Maximum number of retries
  private static final int MAX_RETRIES = 50; // 5 seconds total wait time

  /**
   * Attempts to acquire a lock for the given key
   *
   * @param lockKey the key to lock
   * @return the lock value if acquired, null otherwise
   */
  public String tryLock(String lockKey) {
    String lockValue = generateLockValue();
    Boolean acquired =
        redisTemplate
            .opsForValue()
            .setIfAbsent(lockKey, lockValue, LOCK_TTL_SECONDS, TimeUnit.SECONDS);
    return Boolean.TRUE.equals(acquired) ? lockValue : null;
  }

  /**
   * Releases the lock for the given key
   *
   * @param lockKey the key to unlock
   * @param lockValue the value that was used to acquire the lock
   * @return true if lock was released, false otherwise
   */
  public boolean releaseLock(String lockKey, String lockValue) {
    String script =
        "if redis.call('get', KEYS[1]) == ARGV[1] then "
            + "return redis.call('del', KEYS[1]) "
            + "else return 0 end";

    RedisScript<Long> redisScript = RedisScript.of(script, Long.class);
    Long result = redisTemplate.execute(redisScript, java.util.Arrays.asList(lockKey), lockValue);

    return Long.valueOf(1L).equals(result);
  }

  /**
   * Generates a unique lock value to ensure only the lock owner can release it
   *
   * @return unique lock value
   */
  private String generateLockValue() {
    return Thread.currentThread().getId()
        + ":"
        + System.currentTimeMillis()
        + ":"
        + ThreadLocalRandom.current().nextLong();
  }

  /**
   * Waits for a lock to be released and then attempts to acquire it
   *
   * @param lockKey the key to wait for and lock
   * @return the lock value if acquired, null if failed after max retries
   */
  public String waitForLock(String lockKey) {
    for (int i = 0; i < MAX_RETRIES; i++) {
      String lockValue = generateLockValue();
      Boolean acquired =
          redisTemplate
              .opsForValue()
              .setIfAbsent(lockKey, lockValue, LOCK_TTL_SECONDS, TimeUnit.SECONDS);

      if (Boolean.TRUE.equals(acquired)) {
        log.debug("Lock acquired for key: {} after {} retries", lockKey, i);
        return lockValue;
      }

      try {
        Thread.sleep(RETRY_WAIT_MS);
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
        log.warn("Thread interrupted while waiting for lock: {}", lockKey);
        return null;
      }
    }

    log.warn("Failed to acquire lock for key: {} after {} retries", lockKey, MAX_RETRIES);
    return null;
  }
}
