package com.incidentplatform.user;

import com.incidentplatform.common.dto.PageResponse;
import com.incidentplatform.domain.user.Role;
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
import org.springframework.web.bind.annotation.RequestParam;
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
   * ENGINEER/ADMIN (widened from ADMIN-only in Phase 6/7): an ENGINEER needs this to find who to
   * assign an incident to (FR-8) — there's no other endpoint that lists users. {@code role} lets
   * the assignment picker request only ENGINEER/ADMIN accounts rather than the full directory,
   * which also happens to be the more useful shape for that UI. A plain USER still cannot call this
   * at all; the full unfiltered directory remains something only ADMIN realistically uses.
   */
  @GetMapping
  @PreAuthorize("hasAnyRole('ENGINEER','ADMIN')")
  public PageResponse<UserResponse> listUsers(
      @RequestParam(required = false) Role role, @PageableDefault(size = 20) Pageable pageable) {
    return userService.listUsers(role, pageable);
  }

  /**
   * ADMIN-only (FR-19). Self-role-edit is rejected in the service layer, not here — see
   * UserService.updateRole's Javadoc.
   */
  @PatchMapping("/{id}/role")
  @PreAuthorize("hasRole('ADMIN')")
  public UserResponse updateRole(
      @PathVariable Long id,
      @Valid @RequestBody RoleUpdateRequest request,
      @AuthenticationPrincipal CustomUserDetails principal) {
    return userService.updateRole(id, request, principal.getUserId());
  }
}
