package com.incidentplatform.common.dto;

import java.util.List;
import org.springframework.data.domain.Page;

/** Standard paged response envelope for list endpoints, per Phase 1 §9.1. */
public record PageResponse<T>(
    List<T> content, int page, int size, long totalElements, int totalPages) {

  public static <T> PageResponse<T> of(Page<T> page) {
    return new PageResponse<>(
        page.getContent(),
        page.getNumber(),
        page.getSize(),
        page.getTotalElements(),
        page.getTotalPages());
  }
}
