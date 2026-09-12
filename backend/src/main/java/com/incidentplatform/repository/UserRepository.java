package com.incidentplatform.repository;

import com.incidentplatform.domain.user.Role;
import com.incidentplatform.domain.user.User;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, Long> {

  Optional<User> findByEmail(String email);

  boolean existsByEmail(String email);

  long countByRole(Role role);
}
