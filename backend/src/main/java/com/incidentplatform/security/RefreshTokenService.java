package com.incidentplatform.security;

import java.time.Duration;
import java.util.Optional;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

/**
 * Refresh tokens are opaque random strings stored server-side in Redis (key → user id, with a
 * TTL), not JWTs — see ADR-0008 for the reasoning. This makes them genuinely revocable: {@link
 * #validateAndRevoke(String)} deletes the token as part of validating it, so each refresh token
 * can only ever be used once (rotation), and {@link #revoke(String)} powers logout.
 */
@Service
public class RefreshTokenService {

  private static final String KEY_PREFIX = "refresh-token:";

  private final StringRedisTemplate redisTemplate;
  private final Duration ttl;

  public RefreshTokenService(
      StringRedisTemplate redisTemplate,
      @Value("${app.jwt.refresh-token-ttl-days}") long ttlDays) {
    this.redisTemplate = redisTemplate;
    this.ttl = Duration.ofDays(ttlDays);
  }

  public String issue(Long userId) {
    String token = UUID.randomUUID().toString();
    redisTemplate.opsForValue().set(KEY_PREFIX + token, userId.toString(), ttl);
    return token;
  }

  /**
   * Validates the token and, if valid, deletes it (rotation) so it cannot be replayed. Returns
   * empty if the token doesn't exist or has already expired/been used.
   */
  public Optional<Long> validateAndRevoke(String token) {
    String key = KEY_PREFIX + token;
    String userId = redisTemplate.opsForValue().get(key);
    if (userId == null) {
      return Optional.empty();
    }
    redisTemplate.delete(key);
    return Optional.of(Long.valueOf(userId));
  }

  public void revoke(String token) {
    redisTemplate.delete(KEY_PREFIX + token);
  }
}
