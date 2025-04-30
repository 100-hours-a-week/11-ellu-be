package com.ellu.looper.jwt;

public class JwtExpiration {
  public static final long ACCESS_TOKEN_EXPIRATION = 1000L * 60 * 30; // 30분
  public static final long REFRESH_TOKEN_EXPIRATION = 1000L * 60 * 60 * 24 * 7; // 7일
}
