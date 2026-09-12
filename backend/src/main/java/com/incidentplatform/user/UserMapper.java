package com.incidentplatform.user;

import com.incidentplatform.domain.user.User;
import com.incidentplatform.user.dto.UserResponse;

/** Manual entity↔DTO mapping, per ADR-0007. */
public final class UserMapper {

  private UserMapper() {}

  public static UserResponse toResponse(User user) {
    Long teamId = user.getTeam() != null ? user.getTeam().getId() : null;
    return new UserResponse(
        user.getId(), user.getEmail(), user.getDisplayName(), user.getRole(), teamId,
        user.isActive());
  }
}
