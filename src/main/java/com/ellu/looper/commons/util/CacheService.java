package com.ellu.looper.commons.util;

import java.util.concurrent.ThreadLocalRandom;

public class CacheService {
  public static long addJitter(long baseTtl, double jitterPercent) {
    long jitter = (long) (baseTtl * jitterPercent);
    // Get the current thread's dedicated random number generator
    return baseTtl + ThreadLocalRandom.current().nextLong(0, jitter + 1);
  }
}
