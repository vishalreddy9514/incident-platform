package com.incidentplatform.common.exception;

import org.springframework.http.HttpStatus;

/** Thrown when a requested entity doesn't exist. Maps to 404 with error code {@code NOT_FOUND}. */
public class ResourceNotFoundException extends ApiException {

  public ResourceNotFoundException(String resourceName, Object identifier) {
    super(
        HttpStatus.NOT_FOUND, "NOT_FOUND", "%s not found: %s".formatted(resourceName, identifier));
  }
}
