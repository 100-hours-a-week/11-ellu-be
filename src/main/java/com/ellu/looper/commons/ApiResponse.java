package com.ellu.looper.commons;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class ApiResponse<T> {
  private String message;
  private T data;

  public static <T> ApiResponse<T> success(String message, T data) {
    return new ApiResponse<>(message, data);
  }

  public static ApiResponse<Void> error(String message) {
    return new ApiResponse<>(message, null);
  }
}
