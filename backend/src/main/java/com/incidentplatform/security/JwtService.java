package com.incidentplatform.security;

import com.incidentplatform.domain.user.Role;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import javax.crypto.SecretKey;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * Issues and parses short-lived JWT access tokens. Deliberately takes plain (userId, email, role)
 * rather than the {@code User} entity — this keeps token generation testable without a persisted
 * entity and keeps this class from depending on the JPA/domain layer at all.
 *
 * <p>The signing key must be at least 32 bytes (256 bits) for HS256 — {@code app.jwt.secret}'s
 * default in application.yml is deliberately long enough; a short secret would throw {@code
 * WeakKeyException} at startup, which is the correct failure mode (fail loud, not silently weak).
 */
@Service
public class JwtService {

  private final SecretKey key;
  private final long accessTokenTtlMinutes;

  public JwtService(
      @Value("${app.jwt.secret}") String secret,
      @Value("${app.jwt.access-token-ttl-minutes}") long accessTokenTtlMinutes) {
    this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    this.accessTokenTtlMinutes = accessTokenTtlMinutes;
  }

  public String generateAccessToken(Long userId, String email, Role role) {
    Instant now = Instant.now();
    Instant expiry = now.plus(accessTokenTtlMinutes, ChronoUnit.MINUTES);
    return Jwts.builder()
        .subject(userId.toString())
        .claim("email", email)
        .claim("role", role.name())
        .issuedAt(Date.from(now))
        .expiration(Date.from(expiry))
        .signWith(key, Jwts.SIG.HS256)
        .compact();
  }

  /**
   * Parses and verifies a token, throwing {@link io.jsonwebtoken.JwtException} (or a subclass
   * such as {@code ExpiredJwtException}) if the signature is invalid or the token has expired.
   * Callers (see {@link JwtAuthenticationFilter}) are expected to catch this and treat it as "not
   * authenticated" rather than letting it propagate as a 500.
   */
  public Claims parseClaims(String token) {
    return Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload();
  }

  public long getAccessTokenTtlSeconds() {
    return accessTokenTtlMinutes * 60;
  }
}
