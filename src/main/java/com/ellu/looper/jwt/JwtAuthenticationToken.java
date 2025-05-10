package com.ellu.looper.jwt;

import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.authority.AuthorityUtils;

@Slf4j
public class JwtAuthenticationToken extends AbstractAuthenticationToken {

  private final Long userId;

  public JwtAuthenticationToken(Long userId, Object credentials, Object principal) {
    super(AuthorityUtils.NO_AUTHORITIES);
    this.userId = userId;
    super.setAuthenticated(true); // 무조건 인증 처리
  }

  @Override
  public Object getCredentials() {
    return null;
  }

  @Override
  public Object getPrincipal() {
    return userId;
  }
}
