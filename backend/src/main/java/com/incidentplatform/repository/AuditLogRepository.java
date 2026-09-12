package com.incidentplatform.repository;

import com.incidentplatform.domain.audit.AuditLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {

  // Insert-and-read only, by design (see migration V9's append-only trigger).
  Page<AuditLog> findAllByOrderByCreatedAtDesc(Pageable pageable);
}
