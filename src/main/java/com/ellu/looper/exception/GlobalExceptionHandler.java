package com.ellu.looper.exception;

import com.ellu.looper.commons.ApiResponse;
import java.util.HashMap;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.context.request.async.AsyncRequestTimeoutException;
import org.springframework.web.context.request.async.AsyncRequestNotUsableException;

@ControllerAdvice
public class GlobalExceptionHandler {

  @ExceptionHandler(AccessDeniedException.class)
  public ApiResponse<Void> handleAccessDeniedException(AccessDeniedException ex) {
    return new ApiResponse<>("unauthorized or expired token", null);
  }

  @ExceptionHandler(UserNotFoundException.class)
  public ResponseEntity<ApiResponse<Void>> handleUserNotFound(UserNotFoundException ex) {
    return ResponseEntity.status(HttpStatus.FORBIDDEN)
        .body(new ApiResponse<>("user_not_found_or_unauthorized", null));
  }

  @ExceptionHandler(RuntimeException.class)
  public ResponseEntity<?> handleRuntimeException(RuntimeException ex) {
    return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("message", ex.getMessage()));
  }

  @ExceptionHandler(JwtException.class)
  public ResponseEntity<ErrorResponse> handleJwtException(JwtException ex) {
    return ResponseEntity.status(ex.getStatus()).body(new ErrorResponse(ex.getMessage()));
  }

  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ApiResponse<Map<String, String>> handleValidationErrors(
      MethodArgumentNotValidException ex) {
    Map<String, String> errors = new HashMap<>();
    ex.getBindingResult()
        .getFieldErrors()
        .forEach(error -> errors.put(error.getField(), error.getDefaultMessage()));
    return new ApiResponse<>("validation_failed", errors);
  }

  @ExceptionHandler(IllegalArgumentException.class)
  public ResponseEntity<ApiResponse<Map<String, String>>> handleIllegalArgument(
      IllegalArgumentException ex) {
    Map<String, String> error = Map.of("message", ex.getMessage());
    return ResponseEntity.status(HttpStatus.BAD_REQUEST)
        .body(new ApiResponse<>("validation_failed", error));
  }

  @ExceptionHandler(NicknameAlreadyExistsException.class)
  public ResponseEntity<?> handleNicknameAlreadyExists(NicknameAlreadyExistsException ex) {
    return ResponseEntity.status(HttpStatus.CONFLICT) // 409
        .body(new ApiResponse("nickname_already_exists", null));
  }

  @ExceptionHandler(ValidationException.class)
  public ResponseEntity<?> handleValidationException(ValidationException ex) {
    Map<String, Object> responseBody =
        Map.of("message", "validation_failed", "data", Map.of("errors", ex.getErrors()));
    return ResponseEntity.badRequest().body(responseBody);
  }

  @ExceptionHandler({AsyncRequestTimeoutException.class, AsyncRequestNotUsableException.class})
  public void handleSseAsyncExceptions(Exception ex) {
    String message = ex.getMessage();
    if (message == null) {
      if (ex instanceof AsyncRequestTimeoutException) {
        message = "SSE connection timed out";
      } else if (ex instanceof AsyncRequestNotUsableException) {
        message = "SSE connection became unusable (client disconnected)";
      } else {
        message = "Unknown SSE async error";
      }
    }
    System.err.println("[GlobalExceptionHandler] SSE async error: " + ex.getClass().getName() + ", message=" + message);
  }

  @ExceptionHandler(Exception.class)
  public ResponseEntity<ErrorResponse> handleGenericException(Exception ex) {
    // Detailed logging for debugging
    System.err.println("[GlobalExceptionHandler] Exception caught: type=" + ex.getClass().getName() + ", message=" + ex.getMessage());
    ex.printStackTrace();
    String message = ex.getMessage() != null ? ex.getMessage() : "internal_server_error";
    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
        .body(new ErrorResponse(message));
  }

  @Getter
  @AllArgsConstructor
  static class ErrorResponse {
    private final String message;
  }
}
