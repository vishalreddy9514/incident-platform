package com.incidentplatform.repository;

import com.incidentplatform.domain.user.Role;
import com.incidentplatform.domain.user.User;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, Long> {

  Optional<User> findByEmail(String email);

  boolean existsByEmail(String email);

  long countByRole(Role role);

  /** Backs the ENGINEER/ADMIN-visible "who can I assign this to" list (see UserController) —
   * filtered to a single role rather than paging through every user. */
  Page<User> findByRole(Role role, Pageable pageable);
}
