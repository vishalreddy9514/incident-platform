package com.incidentplatform.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.incidentplatform.domain.incident.IncidentCategory;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * Runs against a real PostgreSQL 16 container, not H2 — {@code @AutoConfigureTestDatabase(replace =
 * NONE)} disables Spring Boot's default swap-in-an-embedded- database behaviour. Flyway runs
 * automatically against the container on context startup (it's on the classpath and enabled by
 * default), and Hibernate's {@code ddl-auto=validate} then checks every entity in this module
 * against the real migrated schema — so this test doubles as end-to-end validation that the Phase 4
 * entities actually match the Phase 3 migrations, not just that the repository method compiles.
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Testcontainers
class IncidentCategoryRepositoryTest {

  @Container
  static final PostgreSQLContainer<?> POSTGRES =
      new PostgreSQLContainer<>("postgres:16-alpine")
          .withDatabaseName("incident_platform_test")
          .withUsername("test")
          .withPassword("test");

  @DynamicPropertySource
  static void datasourceProperties(DynamicPropertyRegistry registry) {
    registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
    registry.add("spring.datasource.username", POSTGRES::getUsername);
    registry.add("spring.datasource.password", POSTGRES::getPassword);
  }

  @Autowired private IncidentCategoryRepository categoryRepository;

  @Test
  void seededCategoriesFromMigrationV11AreFoundAndOrderedByName() {
    List<IncidentCategory> categories = categoryRepository.findByIsActiveTrueOrderByNameAsc();

    assertThat(categories).hasSize(7);
    assertThat(categories.get(0).getName()).isEqualTo("Access & Permissions");
    assertThat(categories).extracting(IncidentCategory::isActive).containsOnly(true);
  }

  @Test
  void savingAndReloadingACategoryRoundTripsCorrectly() {
    IncidentCategory saved =
        categoryRepository.save(new IncidentCategory("Test Category", "Created by a test"));

    IncidentCategory reloaded = categoryRepository.findById(saved.getId()).orElseThrow();

    assertThat(reloaded.getName()).isEqualTo("Test Category");
    assertThat(reloaded.getDescription()).isEqualTo("Created by a test");
    assertThat(reloaded.isActive()).isTrue();
    assertThat(reloaded.getCreatedAt()).isNotNull();
    assertThat(reloaded.getUpdatedAt()).isNotNull();
  }
}
