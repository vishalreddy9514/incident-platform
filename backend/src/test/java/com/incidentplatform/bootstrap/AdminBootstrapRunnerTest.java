package com.incidentplatform.bootstrap;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.incidentplatform.domain.user.Role;
import com.incidentplatform.domain.user.User;
import com.incidentplatform.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
class AdminBootstrapRunnerTest {

  @Mock private UserRepository userRepository;
  @Mock private PasswordEncoder passwordEncoder;

  @Test
  void doesNothingWhenEmailIsBlank() {
    var runner = new AdminBootstrapRunner(userRepository, passwordEncoder, "", "some-password");

    runner.run(null);

    verify(userRepository, never()).save(any());
  }

  @Test
  void doesNothingWhenPasswordIsBlank() {
    var runner = new AdminBootstrapRunner(userRepository, passwordEncoder, "admin@example.com", "");

    runner.run(null);

    verify(userRepository, never()).save(any());
  }

  @Test
  void doesNothingWhenAnAdminAlreadyExists() {
    when(userRepository.countByRole(Role.ADMIN)).thenReturn(1L);
    var runner =
        new AdminBootstrapRunner(userRepository, passwordEncoder, "admin@example.com", "change-me");

    runner.run(null);

    verify(userRepository, never()).save(any());
  }

  @Test
  void doesNothingWhenTheBootstrapEmailIsAlreadyRegistered() {
    when(userRepository.countByRole(Role.ADMIN)).thenReturn(0L);
    when(userRepository.existsByEmail("admin@example.com")).thenReturn(true);
    var runner =
        new AdminBootstrapRunner(userRepository, passwordEncoder, "admin@example.com", "change-me");

    runner.run(null);

    verify(userRepository, never()).save(any());
  }

  @Test
  void createsExactlyOneAdminWhenNoneExistsYet() {
    when(userRepository.countByRole(Role.ADMIN)).thenReturn(0L);
    when(userRepository.existsByEmail("admin@example.com")).thenReturn(false);
    when(passwordEncoder.encode("change-me")).thenReturn("hashed-password");
    var runner =
        new AdminBootstrapRunner(userRepository, passwordEncoder, "admin@example.com", "change-me");

    runner.run(null);

    ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
    verify(userRepository).save(captor.capture());
    User created = captor.getValue();
    assertThat(created.getEmail()).isEqualTo("admin@example.com");
    assertThat(created.getPasswordHash()).isEqualTo("hashed-password");
    assertThat(created.getRole()).isEqualTo(Role.ADMIN);
  }
}
