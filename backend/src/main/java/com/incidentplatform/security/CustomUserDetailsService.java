package com.incidentplatform.security;

import com.incidentplatform.domain.user.User;
import com.incidentplatform.repository.UserRepository;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

/**
 * Loads a {@link User} by email (used as the Spring Security "username") and wraps it in {@link
 * CustomUserDetails}. Used by {@link JwtAuthenticationFilter} on every authenticated request to
 * re-load the current state of the user (so a deactivated account stops working immediately, not
 * just after its access token expires).
 */
@Service
public class CustomUserDetailsService implements UserDetailsService {

  private final UserRepository userRepository;

  public CustomUserDetailsService(UserRepository userRepository) {
    this.userRepository = userRepository;
  }

  @Override
  public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
    User user =
        userRepository
            .findByEmail(email)
            .orElseThrow(() -> new UsernameNotFoundException("No user with email: " + email));
    return new CustomUserDetails(user);
  }
}
