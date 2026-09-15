package com.incidentplatform.ai;

import com.incidentplatform.ai.dto.AiAnalysisResponse;
import com.incidentplatform.ai.dto.AiAnalysisResult;
import com.incidentplatform.ai.dto.AnalyseIncidentRequest;
import com.incidentplatform.common.exception.ResourceNotFoundException;
import com.incidentplatform.domain.ai.AiAnalysis;
import com.incidentplatform.domain.incident.Incident;
import com.incidentplatform.incident.IncidentService;
import com.incidentplatform.repository.AiAnalysisRepository;
import com.incidentplatform.security.CustomUserDetails;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Requests and stores AI-assisted analysis for an incident (FR-13). Advisory only (FR-14) — this
 * never mutates the incident itself, and a failure here (see {@link AiAnalysisClient}) doesn't
 * affect any other incident operation (FR-15).
 */
@Service
public class AiAnalysisService {

  private final AiAnalysisClient client;
  private final AiAnalysisRepository aiAnalysisRepository;
  private final IncidentService incidentService;

  AiAnalysisService(
      AiAnalysisClient client,
      AiAnalysisRepository aiAnalysisRepository,
      IncidentService incidentService) {
    this.client = client;
    this.aiAnalysisRepository = aiAnalysisRepository;
    this.incidentService = incidentService;
  }

  @Transactional
  public AiAnalysisResponse analyse(Long incidentId, CustomUserDetails principal) {
    Incident incident = incidentService.requireViewableIncident(incidentId, principal);

    AnalyseIncidentRequest request =
        new AnalyseIncidentRequest(
            incident.getTitle(), incident.getDescription(), incident.getCategory().getName());
    AiAnalysisResult result = client.analyse(request);

    AiAnalysis analysis =
        new AiAnalysis(
            incident,
            result.suggestedCategory(),
            result.predictedPriority(),
            result.summary(),
            AiAnalysisMapper.toJson(result.keywords()),
            AiAnalysisMapper.toJson(result.suggestedSteps()),
            result.modelUsed());
    aiAnalysisRepository.save(analysis);

    return AiAnalysisMapper.toResponse(analysis);
  }

  @Transactional(readOnly = true)
  public AiAnalysisResponse getLatest(Long incidentId, CustomUserDetails principal) {
    incidentService.requireViewableIncident(incidentId, principal);

    return aiAnalysisRepository.findByIncidentIdOrderByCreatedAtDesc(incidentId).stream()
        .findFirst()
        .map(AiAnalysisMapper::toResponse)
        .orElseThrow(() -> new ResourceNotFoundException("AiAnalysis", incidentId));
  }
}
