package com.incidentplatform.category.dto;

/** What the API returns for a category — deliberately smaller than the entity (no timestamps). */
public record CategoryResponse(Long id, String name, String description) {}
