package com.incidentplatform.common.dto;

/**
 * Standard error envelope for every non-2xx response, per Phase 1 §9.1: {@code {"error": {"code",
 * "message", "details"}}}. {@code details} is intentionally {@code Object} to accommodate both a
 * simple null and structured validation failures (see {@link
 * com.incidentplatform.common.exception.GlobalExceptionHandler}).
 */
public record ErrorResponse(ErrorDetail error) {

  public static ErrorResponse of(String code, String message) {
    return new ErrorResponse(new ErrorDetail(code, message, null));
  }

  public static ErrorResponse of(String code, String message, Object details) {
    return new ErrorResponse(new ErrorDetail(code, message, details));
  }

  public record ErrorDetail(String code, String message, Object details) {}
}
