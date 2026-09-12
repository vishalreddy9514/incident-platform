package com.incidentplatform.repository;

import com.incidentplatform.domain.ai.AiAnalysis;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AiAnalysisRepository extends JpaRepository<AiAnalysis, Long> {

  List<AiAnalysis> findByIncidentIdOrderByCreatedAtDesc(Long incidentId);
}
