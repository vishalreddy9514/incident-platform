package com.incidentplatform.user;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.incidentplatform.common.exception.ResourceNotFoundException;
import com.incidentplatform.domain.user.Role;
import com.incidentplatform.domain.user.User;
import com.incidentplatform.repository.UserRepository;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

  @Mock private UserRepository userRepository;

  @Test
  void getProfileReturnsTheMappedUser() {
    User user = new User("user@example.com", "hashed", "User Name", Role.ENGINEER);
    when(userRepository.findById(5L)).thenReturn(Optional.of(user));

    var response = new UserService(userRepository).getProfile(5L);

    assertThat(response.email()).isEqualTo("user@example.com");
    assertThat(response.role()).isEqualTo(Role.ENGINEER);
  }

  @Test
  void getProfileThrowsWhenUserDoesNotExist() {
    when(userRepository.findById(99L)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> new UserService(userRepository).getProfile(99L))
        .isInstanceOf(ResourceNotFoundException.class);
  }

  @Test
  void listUsersMapsAPagedResult() {
    User user = new User("admin@example.com", "hashed", "Admin", Role.ADMIN);
    var pageRequest = PageRequest.of(0, 20);
    when(userRepository.findAll(pageRequest)).thenReturn(new PageImpl<>(List.of(user), pageRequest, 1));

    var response = new UserService(userRepository).listUsers(pageRequest);

    assertThat(response.content()).hasSize(1);
    assertThat(response.content().get(0).email()).isEqualTo("admin@example.com");
    assertThat(response.totalElements()).isEqualTo(1);
  }
}
