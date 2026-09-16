package com.incidentplatform.migration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import javax.sql.DataSource;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.postgresql.ds.PGSimpleDataSource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * Verifies the Flyway migrations in {@code db/migration} apply cleanly against a real PostgreSQL
 * instance, and that the schema behaves the way Phase 3 designed it to: role/status CHECK
 * constraints reject invalid values, and the append-only tables genuinely refuse UPDATE and DELETE
 * rather than merely documenting that intent.
 *
 * <p>Deliberately does not boot the Spring context (that arrives in Phase 4) — this test only needs
 * Flyway and a JDBC connection, so it stays fast and focused on the migrations themselves.
 */
@Testcontainers
class FlywayMigrationTest {

  @Container
  static final PostgreSQLContainer<?> POSTGRES =
      new PostgreSQLContainer<>("postgres:16-alpine")
          .withDatabaseName("incident_platform_test")
          .withUsername("test")
          .withPassword("test");

  static DataSource dataSource;

  @BeforeAll
  static void migrate() {
    PGSimpleDataSource ds = new PGSimpleDataSource();
    ds.setUrl(POSTGRES.getJdbcUrl());
    ds.setUser(POSTGRES.getUsername());
    ds.setPassword(POSTGRES.getPassword());
    dataSource = ds;

    Flyway.configure().dataSource(dataSource).load().migrate();
  }

  @Test
  void allExpectedTablesExist() throws SQLException {
    String[] expectedTables = {
      "teams",
      "users",
      "incident_categories",
      "incidents",
      "incident_comments",
      "incident_assignments",
      "incident_history",
      "audit_logs",
      "ai_analysis"
    };

    try (Connection conn = dataSource.getConnection();
        Statement stmt = conn.createStatement();
        ResultSet rs =
            stmt.executeQuery(
                "SELECT table_name FROM information_schema.tables WHERE table_schema = 'public'")) {
      var actualTables = new java.util.HashSet<String>();
      while (rs.next()) {
        actualTables.add(rs.getString("table_name"));
      }
      assertThat(actualTables).contains(expectedTables);
    }
  }

  @Test
  void incidentCategoriesAreSeeded() throws SQLException {
    try (Connection conn = dataSource.getConnection();
        Statement stmt = conn.createStatement();
        ResultSet rs = stmt.executeQuery("SELECT count(*) FROM incident_categories")) {
      rs.next();
      assertThat(rs.getInt(1)).isEqualTo(7);
    }
  }

  @Test
  void userRoleCheckConstraintRejectsInvalidRole() {
    assertThatThrownBy(
            () -> {
              try (Connection conn = dataSource.getConnection();
                  PreparedStatement ps =
                      conn.prepareStatement(
                          "INSERT INTO users (email, password_hash, display_name, role) "
                              + "VALUES (?, ?, ?, ?)")) {
                ps.setString(1, "invalid-role@example.com");
                ps.setString(2, "hash");
                ps.setString(3, "Invalid Role User");
                ps.setString(4, "SUPERADMIN");
                ps.executeUpdate();
              }
            })
        .isInstanceOf(SQLException.class)
        .hasMessageContaining("chk_users_role");
  }

  @Test
  void incidentHistoryRejectsUpdateAndDelete() throws SQLException {
    long userId = insertUser("history-actor@example.com", "USER");
    long incidentId = insertIncident("Test incident", userId);
    long historyId = insertHistoryRow(incidentId, userId);

    assertThatThrownBy(
            () -> {
              try (Connection conn = dataSource.getConnection();
                  Statement stmt = conn.createStatement()) {
                stmt.executeUpdate(
                    "UPDATE incident_history SET new_value = 'TAMPERED' WHERE id = " + historyId);
              }
            })
        .isInstanceOf(SQLException.class)
        .hasMessageContaining("append-only");

    assertThatThrownBy(
            () -> {
              try (Connection conn = dataSource.getConnection();
                  Statement stmt = conn.createStatement()) {
                stmt.executeUpdate("DELETE FROM incident_history WHERE id = " + historyId);
              }
            })
        .isInstanceOf(SQLException.class)
        .hasMessageContaining("append-only");
  }

  private long insertUser(String email, String role) throws SQLException {
    try (Connection conn = dataSource.getConnection();
        PreparedStatement ps =
            conn.prepareStatement(
                "INSERT INTO users (email, password_hash, display_name, role) "
                    + "VALUES (?, 'hash', 'Test User', ?) RETURNING id")) {
      ps.setString(1, email);
      ps.setString(2, role);
      try (ResultSet rs = ps.executeQuery()) {
        rs.next();
        return rs.getLong(1);
      }
    }
  }

  private long insertIncident(String title, long createdById) throws SQLException {
    try (Connection conn = dataSource.getConnection();
        PreparedStatement ps =
            conn.prepareStatement(
                "INSERT INTO incidents (title, description, category_id, created_by_id) "
                    + "VALUES (?, 'description', 1, ?) RETURNING id")) {
      ps.setString(1, title);
      ps.setLong(2, createdById);
      try (ResultSet rs = ps.executeQuery()) {
        rs.next();
        return rs.getLong(1);
      }
    }
  }

  private long insertHistoryRow(long incidentId, long actorId) throws SQLException {
    try (Connection conn = dataSource.getConnection();
        PreparedStatement ps =
            conn.prepareStatement(
                "INSERT INTO incident_history (incident_id, actor_id, field_changed, new_value) "
                    + "VALUES (?, ?, 'status', 'OPEN') RETURNING id")) {
      ps.setLong(1, incidentId);
      ps.setLong(2, actorId);
      try (ResultSet rs = ps.executeQuery()) {
        rs.next();
        return rs.getLong(1);
      }
    }
  }
}
