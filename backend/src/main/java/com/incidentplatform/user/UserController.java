package com.incidentplatform.user;

import com.incidentplatform.common.dto.PageResponse;
import com.incidentplatform.security.CustomUserDetails;
import com.incidentplatform.user.dto.RoleUpdateRequest;
import com.incidentplatform.user.dto.UserResponse;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/users")
public class UserController {

  private final UserService userService;

  public UserController(UserService userService) {
    this.userService = userService;
  }

  /** Any authenticated user can view their own profile — no role gate needed. */
  @GetMapping("/me")
  public UserResponse getCurrentUser(@AuthenticationPrincipal CustomUserDetails principal) {
    return userService.getProfile(principal.getUserId());
  }

  /**
   * ADMIN-only (FR-19). The explicit role gate here, rather than a hierarchical check, follows
   * ADR-0002 — this is a plain role-based permission with no ownership dimension.
   */
  @GetMapping
  @PreAuthorize("hasRole('ADMIN')")
  public PageResponse<UserResponse> listUsers(@PageableDefault(size = 20) Pageable pageable) {
    return userService.listUsers(pageable);
  }

  /** ADMIN-only (FR-19). Self-role-edit is rejected in the service layer, not here — see
   * UserService.updateRole's Javadoc. */
  @PatchMapping("/{id}/role")
  @PreAuthorize("hasRole('ADMIN')")
  public UserResponse updateRole(
      @PathVariable Long id,
      @Valid @RequestBody RoleUpdateRequest request,
      @AuthenticationPrincipal CustomUserDetails principal) {
    return userService.updateRole(id, request, principal.getUserId());
  }
}
