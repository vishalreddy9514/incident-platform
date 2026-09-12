package com.incidentplatform.user;

import com.incidentplatform.common.dto.PageResponse;
import com.incidentplatform.common.exception.ResourceNotFoundException;
import com.incidentplatform.domain.user.User;
import com.incidentplatform.repository.UserRepository;
import com.incidentplatform.user.dto.UserResponse;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Service
public class UserService {

  private final UserRepository userRepository;

  public UserService(UserRepository userRepository) {
    this.userRepository = userRepository;
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
}
