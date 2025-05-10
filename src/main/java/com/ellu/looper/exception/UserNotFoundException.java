package com.ellu.looper.exception;

public class UserNotFoundException extends RuntimeException {
  public UserNotFoundException(String message) {
    super(message);
  }
}

// User user = userRepository.findById(userId)
//    .orElseThrow(() -> new UserNotFoundException("User not found or unauthorized"));
