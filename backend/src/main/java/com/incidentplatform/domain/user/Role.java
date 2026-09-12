package com.incidentplatform.domain.user;

/**
 * Matches the {@code chk_users_role} CHECK constraint (migration V3) and ADR-0003/ADR-0006:
 * modelled as a fixed, small set rather than a normalised permissions table.
 */
public enum Role {
  USER,
  ENGINEER,
  ADMIN
}
