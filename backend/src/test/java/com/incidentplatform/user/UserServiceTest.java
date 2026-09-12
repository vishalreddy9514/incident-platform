package com.incidentplatform.user;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.incidentplatform.common.exception.ApiException;
import com.incidentplatform.common.exception.ResourceNotFoundException;
import com.incidentplatform.domain.audit.AuditLog;
import com.incidentplatform.domain.user.Role;
import com.incidentplatform.domain.user.User;
import com.incidentplatform.repository.AuditLogRepository;
import com.incidentplatform.repository.UserRepository;
import com.incidentplatform.user.dto.RoleUpdateRequest;
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
  @Mock private AuditLogRepository auditLogRepository;

  private UserService service() {
    return new UserService(userRepository, auditLogRepository);
  }

  @Test
  void getProfileReturnsTheMappedUser() {
    User user = new User("user@example.com", "hashed", "User Name", Role.ENGINEER);
    when(userRepository.findById(5L)).thenReturn(Optional.of(user));

    var response = service().getProfile(5L);

    assertThat(response.email()).isEqualTo("user@example.com");
    assertThat(response.role()).isEqualTo(Role.ENGINEER);
  }

  @Test
  void getProfileThrowsWhenUserDoesNotExist() {
    when(userRepository.findById(99L)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> service().getProfile(99L)).isInstanceOf(ResourceNotFoundException.class);
  }

  @Test
  void listUsersMapsAPagedResult() {
    User user = new User("admin@example.com", "hashed", "Admin", Role.ADMIN);
    var pageRequest = PageRequest.of(0, 20);
    when(userRepository.findAll(pageRequest)).thenReturn(new PageImpl<>(List.of(user), pageRequest, 1));

    var response = service().listUsers(null, pageRequest);

    assertThat(response.content()).hasSize(1);
    assertThat(response.content().get(0).email()).isEqualTo("admin@example.com");
    assertThat(response.totalElements()).isEqualTo(1);
  }

  @Test
  void listUsersFiltersByRoleWhenProvided() {
    User engineer = new User("engineer@example.com", "hashed", "Engineer", Role.ENGINEER);
    var pageRequest = PageRequest.of(0, 20);
    when(userRepository.findByRole(Role.ENGINEER, pageRequest))
        .thenReturn(new PageImpl<>(List.of(engineer), pageRequest, 1));

    var response = service().listUsers(Role.ENGINEER, pageRequest);

    assertThat(response.content()).hasSize(1);
    assertThat(response.content().get(0).role()).isEqualTo(Role.ENGINEER);
  }

  @Test
  void updateRoleChangesTheTargetUsersRoleAndWritesAnAuditLogEntry() {
    User target = new User("target@example.com", "hashed", "Target User", Role.USER);
    User actor = new User("admin@example.com", "hashed", "Admin", Role.ADMIN);
    when(userRepository.findById(5L)).thenReturn(Optional.of(target));
    when(userRepository.findById(1L)).thenReturn(Optional.of(actor));

    var response = service().updateRole(5L, new RoleUpdateRequest(Role.ENGINEER), 1L);

    assertThat(response.role()).isEqualTo(Role.ENGINEER);
    assertThat(target.getRole()).isEqualTo(Role.ENGINEER);
    verify(auditLogRepository).save(org.mockito.ArgumentMatchers.any(AuditLog.class));
  }

  @Test
  void updateRoleRejectsChangingYourOwnRole() {
    assertThatThrownBy(() -> service().updateRole(1L, new RoleUpdateRequest(Role.ADMIN), 1L))
        .isInstanceOf(ApiException.class)
        .extracting(ex -> ((ApiException) ex).getErrorCode())
        .isEqualTo("CANNOT_CHANGE_OWN_ROLE");
  }

  @Test
  void updateRoleThrowsWhenTargetUserDoesNotExist() {
    when(userRepository.findById(99L)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> service().updateRole(99L, new RoleUpdateRequest(Role.ENGINEER), 1L))
        .isInstanceOf(ResourceNotFoundException.class);
  }
}
