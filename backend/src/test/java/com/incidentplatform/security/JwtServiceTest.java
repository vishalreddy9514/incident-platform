package com.incidentplatform.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.incidentplatform.domain.user.Role;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.security.SignatureException;
import org.junit.jupiter.api.Test;

class JwtServiceTest {

  // 40+ chars: comfortably over the 32-byte minimum HS256 requires.
  private static final String TEST_SECRET = "test-secret-key-that-is-long-enough-for-hs256-signing";

  @Test
  void generatesATokenContainingTheExpectedClaims() {
    JwtService jwtService = new JwtService(TEST_SECRET, 15);

    String token = jwtService.generateAccessToken(42L, "engineer@example.com", Role.ENGINEER);
    Claims claims = jwtService.parseClaims(token);

    assertThat(claims.getSubject()).isEqualTo("42");
    assertThat(claims.get("email", String.class)).isEqualTo("engineer@example.com");
    assertThat(claims.get("role", String.class)).isEqualTo("ENGINEER");
    assertThat(claims.getExpiration()).isAfter(claims.getIssuedAt());
  }

  @Test
  void rejectsATokenSignedWithADifferentKey() {
    JwtService issuer = new JwtService(TEST_SECRET, 15);
    JwtService verifier = new JwtService("a-completely-different-secret-key-also-over-32-bytes", 15);

    String token = issuer.generateAccessToken(1L, "user@example.com", Role.USER);

    assertThatThrownBy(() -> verifier.parseClaims(token)).isInstanceOf(SignatureException.class);
  }

  @Test
  void rejectsAnExpiredToken() {
    // TTL of -1 minute: the expiry is already in the past the moment the token is issued.
    JwtService jwtService = new JwtService(TEST_SECRET, -1);

    String token = jwtService.generateAccessToken(1L, "user@example.com", Role.USER);

    assertThatThrownBy(() -> jwtService.parseClaims(token)).isInstanceOf(ExpiredJwtException.class);
  }

  @Test
  void reportsAccessTokenTtlInSeconds() {
    JwtService jwtService = new JwtService(TEST_SECRET, 15);

    assertThat(jwtService.getAccessTokenTtlSeconds()).isEqualTo(15 * 60L);
  }
}
