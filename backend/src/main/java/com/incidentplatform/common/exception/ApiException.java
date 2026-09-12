package com.incidentplatform.common.exception;

import org.springframework.http.HttpStatus;

/**
 * Base type for exceptions that should be translated into the standard error envelope by {@link
 * GlobalExceptionHandler}, rather than surfacing as an unhandled 500. Feature-specific exceptions
 * (added from Phase 5 onward) extend this rather than each writing their own {@code
 * @ExceptionHandler}.
 */
public class ApiException extends RuntimeException {

  private final HttpStatus status;
  private final String errorCode;

  public ApiException(HttpStatus status, String errorCode, String message) {
    super(message);
    this.status = status;
    this.errorCode = errorCode;
  }

  public HttpStatus getStatus() {
    return status;
  }

  public String getErrorCode() {
    return errorCode;
  }
}
