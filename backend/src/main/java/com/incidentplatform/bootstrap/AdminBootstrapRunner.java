package com.incidentplatform.bootstrap;

import com.incidentplatform.domain.user.Role;
import com.incidentplatform.domain.user.User;
import com.incidentplatform.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * Solves a real bootstrapping problem: every ADMIN-only endpoint (role changes, category
 * management, user listing) needs an ADMIN to exist, but registration only ever creates USER
 * accounts (Phase 5) and there's no seeded admin in the migrations — seeding a real user account in
 * a Flyway migration would be demo/fake data masquerading as production schema, which migration
 * V11's own comment explicitly avoided for categories.
 *
 * <p>Instead: on startup, if no ADMIN account exists yet and {@code
 * app.admin-bootstrap.email}/{@code .password} are configured (non-blank), create exactly one.
 * Idempotent — checked via a cheap {@code COUNT} query — so it only ever creates the account once,
 * and does nothing at all if the env vars are left unset (the sensible default for an environment
 * where the first admin is created some other way).
 */
@Component
public class AdminBootstrapRunner implements ApplicationRunner {

  private static final Logger log = LoggerFactory.getLogger(AdminBootstrapRunner.class);

  private final UserRepository userRepository;
  private final PasswordEncoder passwordEncoder;
  private final String bootstrapEmail;
  private final String bootstrapPassword;

  public AdminBootstrapRunner(
      UserRepository userRepository,
      PasswordEncoder passwordEncoder,
      @Value("${app.admin-bootstrap.email:}") String bootstrapEmail,
      @Value("${app.admin-bootstrap.password:}") String bootstrapPassword) {
    this.userRepository = userRepository;
    this.passwordEncoder = passwordEncoder;
    this.bootstrapEmail = bootstrapEmail;
    this.bootstrapPassword = bootstrapPassword;
  }

  @Override
  public void run(ApplicationArguments args) {
    if (bootstrapEmail.isBlank() || bootstrapPassword.isBlank()) {
      return;
    }
    if (userRepository.countByRole(Role.ADMIN) > 0) {
      return;
    }
    if (userRepository.existsByEmail(bootstrapEmail)) {
      log.warn(
          "ADMIN_BOOTSTRAP_EMAIL is already registered as a non-admin account; skipping bootstrap."
              + " Promote it manually via PATCH /api/v1/users/{id}/role once another admin exists,"
              + " or remove the conflicting account.");
      return;
    }
    User admin =
        new User(bootstrapEmail, passwordEncoder.encode(bootstrapPassword), "Admin", Role.ADMIN);
    userRepository.save(admin);
    log.info("Bootstrap admin account created: {}", bootstrapEmail);
  }
}
