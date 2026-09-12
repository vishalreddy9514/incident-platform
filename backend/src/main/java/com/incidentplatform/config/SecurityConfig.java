package com.incidentplatform.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;

/**
 * TEMPORARY, Phase 4 only. Spring Security is on the classpath (added in Phase 2, in preparation
 * for JWT authentication) and, left unconfigured, would default to HTTP Basic with a randomly
 * generated password on every request — which would block even the ability to exercise the
 * skeleton endpoints built in this phase.
 *
 * <p>This configuration permits all requests unauthenticated purely so the application is
 * runnable and testable during Phase 4. It is replaced in Phase 5 by JWT-based authentication and
 * the explicit per-action RBAC rules described in ADR-0002 — every endpoint added from Phase 5
 * onward gets a real, deliberate access rule, not this blanket permit-all.
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

  @Bean
  public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
    http.csrf(csrf -> csrf.disable())
        .authorizeHttpRequests(auth -> auth.anyRequest().permitAll());
    return http.build();
  }
}
