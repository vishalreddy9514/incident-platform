package com.incidentplatform.user;

import com.incidentplatform.common.dto.PageResponse;
import com.incidentplatform.common.exception.ApiException;
import com.incidentplatform.common.exception.ResourceNotFoundException;
import com.incidentplatform.domain.audit.AuditLog;
import com.incidentplatform.domain.user.User;
import com.incidentplatform.repository.AuditLogRepository;
import com.incidentplatform.repository.UserRepository;
import com.incidentplatform.user.dto.RoleUpdateRequest;
import com.incidentplatform.user.dto.UserResponse;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserService {

  private final UserRepository userRepository;
  private final AuditLogRepository auditLogRepository;

  public UserService(UserRepository userRepository, AuditLogRepository auditLogRepository) {
    this.userRepository = userRepository;
    this.auditLogRepository = auditLogRepository;
  }

  public UserResponse getProfile(Long userId) {
    User user =
        userRepository.findById(userId).orElseThrow(() -> new ResourceNotFoundException("User", userId));
    return UserMapper.toResponse(user);
  }

  /**
   * ADMIN-only per FR-19; the role gate is applied at the controller via {@code @PreAuthorize}
   * (ADR-0002), not here — this method has no ownership dimension to enforce, just the role gate.
   */
  public PageResponse<UserResponse> listUsers(Pageable pageable) {
    return PageResponse.of(userRepository.findAll(pageable).map(UserMapper::toResponse));
  }

  /**
   * Changes a user's role (FR-19). Self-role-edit is deliberately blocked — not because an admin
   * demoting themselves is inherently wrong, but because it's the simplest rule that makes
   * accidentally locking every admin out of the system structurally impossible, without needing
   * to compute "is this the last remaining admin" on every request.
   */
  @Transactional
  public UserResponse updateRole(Long targetUserId, RoleUpdateRequest request, Long actingUserId) {
    if (targetUserId.equals(actingUserId)) {
      throw new ApiException(
          HttpStatus.BAD_REQUEST, "CANNOT_CHANGE_OWN_ROLE", "You cannot change your own role");
    }
    User target =
        userRepository
            .findById(targetUserId)
            .orElseThrow(() -> new ResourceNotFoundException("User", targetUserId));
    User actor =
        userRepository
            .findById(actingUserId)
            .orElseThrow(() -> new ResourceNotFoundException("User", actingUserId));

    String previousRole = target.getRole().name();
    target.setRole(request.role());

    String metadata =
        "{\"previousRole\":\"%s\",\"newRole\":\"%s\"}".formatted(previousRole, request.role().name());
    auditLogRepository.save(new AuditLog(actor, "USER_ROLE_CHANGED", "User", target.getId(), metadata));

    return UserMapper.toResponse(target);
  }
}
