package com.ellu.looper.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class LoginResponse {

  private String accessToken;

  private boolean new_user;
}
