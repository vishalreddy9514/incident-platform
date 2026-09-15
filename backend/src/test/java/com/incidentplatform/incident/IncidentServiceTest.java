package com.incidentplatform.incident;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.incidentplatform.common.exception.ApiException;
import com.incidentplatform.domain.incident.Incident;
import com.incidentplatform.domain.incident.IncidentCategory;
import com.incidentplatform.domain.incident.IncidentStatus;
import com.incidentplatform.domain.user.Role;
import com.incidentplatform.domain.user.User;
import com.incidentplatform.incident.dto.AssignRequest;
import com.incidentplatform.incident.dto.CreateIncidentRequest;
import com.incidentplatform.incident.dto.UpdateIncidentRequest;
import com.incidentplatform.repository.AuditLogRepository;
import com.incidentplatform.repository.IncidentAssignmentRepository;
import com.incidentplatform.repository.IncidentCategoryRepository;
import com.incidentplatform.repository.IncidentCommentRepository;
import com.incidentplatform.repository.IncidentHistoryRepository;
import com.incidentplatform.repository.IncidentRepository;
import com.incidentplatform.repository.UserRepository;
import com.incidentplatform.security.CustomUserDetails;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class IncidentServiceTest {

  @Mock private IncidentRepository incidentRepository;
  @Mock private IncidentCategoryRepository categoryRepository;
  @Mock private UserRepository userRepository;
  @Mock private IncidentCommentRepository commentRepository;
  @Mock private IncidentAssignmentRepository assignmentRepository;
  @Mock private IncidentHistoryRepository historyRepository;
  @Mock private AuditLogRepository auditLogRepository;

  private IncidentService service;
  private IncidentCategory category;
  private User creator;
  private User engineer;
  private CustomUserDetails creatorPrincipal;
  private CustomUserDetails engineerPrincipal;

  @BeforeEach
  void setUp() {
    service =
        new IncidentService(
            incidentRepository,
            categoryRepository,
            userRepository,
            commentRepository,
            assignmentRepository,
            historyRepository,
            auditLogRepository);
    category = new IncidentCategory("Hardware", "Physical equipment issues");
    creator = new User("creator@example.com", "hashed", "Creator", Role.USER);
    engineer = new User("engineer@example.com", "hashed", "Engineer", Role.ENGINEER);
    // IDs are DB-generated in production, so a persisted entity always has one; assign them here
    // too, since ownership checks (assertCanView/update) call .getId().equals(...) and every real
    // Incident/User this service touches has already been loaded from a repository.
    ReflectionTestUtils.setField(category, "id", 1L);
    ReflectionTestUtils.setField(creator, "id", 100L);
    ReflectionTestUtils.setField(engineer, "id", 200L);
    creatorPrincipal = new CustomUserDetails(creator);
    engineerPrincipal = new CustomUserDetails(engineer);
  }

  @Test
  void createBuildsAnOpenIncidentAndRecordsHistory() {
    when(categoryRepository.findById(1L)).thenReturn(Optional.of(category));
    when(userRepository.findById(any())).thenReturn(Optional.of(creator));
    when(incidentRepository.save(any(Incident.class))).thenAnswer(inv -> inv.getArgument(0));

    var response =
        service.create(
            new CreateIncidentRequest("Laptop won't boot", "Black screen on startup", 1L),
            creatorPrincipal);

    assertThat(response.title()).isEqualTo("Laptop won't boot");
    assertThat(response.status()).isEqualTo(IncidentStatus.OPEN);
    assertThat(response.categoryName()).isEqualTo("Hardware");
  }

  @Test
  void aUserCannotViewAnIncidentTheyDidNotCreate() {
    Incident incident = new Incident("Title", "Description", category, engineer);
    when(incidentRepository.findById(10L)).thenReturn(Optional.of(incident));

    assertThatThrownBy(() -> service.getById(10L, creatorPrincipal))
        .isInstanceOf(AccessDeniedException.class);
  }

  @Test
  void anEngineerCanViewAnyIncident() {
    Incident incident = new Incident("Title", "Description", category, creator);
    when(incidentRepository.findById(10L)).thenReturn(Optional.of(incident));

    var response = service.getById(10L, engineerPrincipal);

    assertThat(response.title()).isEqualTo("Title");
  }

  @Test
  void aUserCanOnlyEditTheirOwnIncidentWhileItIsStillOpen() {
    Incident incident = new Incident("Title", "Description", category, creator);
    incident.setStatus(IncidentStatus.IN_PROGRESS);
    when(incidentRepository.findById(10L)).thenReturn(Optional.of(incident));

    UpdateIncidentRequest request =
        new UpdateIncidentRequest("New title", null, null, null, null, null);

    assertThatThrownBy(() -> service.update(10L, request, creatorPrincipal))
        .isInstanceOf(AccessDeniedException.class);
  }

  @Test
  void aUserCannotChangeStatusEvenOnTheirOwnOpenIncident() {
    Incident incident = new Incident("Title", "Description", category, creator);
    when(incidentRepository.findById(10L)).thenReturn(Optional.of(incident));

    UpdateIncidentRequest request =
        new UpdateIncidentRequest(null, null, null, IncidentStatus.IN_PROGRESS, null, null);

    assertThatThrownBy(() -> service.update(10L, request, creatorPrincipal))
        .isInstanceOf(AccessDeniedException.class);
  }

  @Test
  void anEngineerCanChangeStatusThroughAValidTransition() {
    Incident incident = new Incident("Title", "Description", category, creator);
    when(incidentRepository.findById(10L)).thenReturn(Optional.of(incident));
    when(userRepository.findById(any())).thenReturn(Optional.of(engineer));

    UpdateIncidentRequest request =
        new UpdateIncidentRequest(null, null, null, IncidentStatus.IN_PROGRESS, null, null);

    var response = service.update(10L, request, engineerPrincipal);

    assertThat(response.status()).isEqualTo(IncidentStatus.IN_PROGRESS);
  }

  @Test
  void anInvalidStatusTransitionIsRejected() {
    Incident incident = new Incident("Title", "Description", category, creator);
    incident.setStatus(IncidentStatus.CLOSED);
    when(incidentRepository.findById(10L)).thenReturn(Optional.of(incident));
    when(userRepository.findById(any())).thenReturn(Optional.of(engineer));

    UpdateIncidentRequest request =
        new UpdateIncidentRequest(null, null, null, IncidentStatus.OPEN, null, null);

    assertThatThrownBy(() -> service.update(10L, request, engineerPrincipal))
        .isInstanceOf(ApiException.class)
        .extracting(ex -> ((ApiException) ex).getErrorCode())
        .isEqualTo("INVALID_STATUS_TRANSITION");
  }

  @Test
  void assigningToARegularUserIsRejected() {
    Incident incident = new Incident("Title", "Description", category, creator);
    when(incidentRepository.findById(10L)).thenReturn(Optional.of(incident));
    when(userRepository.findById(99L)).thenReturn(Optional.of(creator)); // a USER, not ENGINEER/ADMIN

    assertThatThrownBy(
            () -> service.assign(10L, new AssignRequest(99L), engineerPrincipal))
        .isInstanceOf(ApiException.class)
        .extracting(ex -> ((ApiException) ex).getErrorCode())
        .isEqualTo("INVALID_ASSIGNEE");
  }

  @Test
  void assigningAnOpenIncidentMovesItToInProgress() {
    Incident incident = new Incident("Title", "Description", category, creator);
    when(incidentRepository.findById(10L)).thenReturn(Optional.of(incident));
    when(userRepository.findById(99L)).thenReturn(Optional.of(engineer));
    when(userRepository.findById(engineerPrincipal.getUserId())).thenReturn(Optional.of(engineer));
    when(assignmentRepository.findByIncidentIdOrderByAssignedAtDesc(10L)).thenReturn(List.of());

    var response = service.assign(10L, new AssignRequest(99L), engineerPrincipal);

    assertThat(response.assignedToDisplayName()).isEqualTo("Engineer");
    assertThat(response.status()).isEqualTo(IncidentStatus.IN_PROGRESS);
  }
}
