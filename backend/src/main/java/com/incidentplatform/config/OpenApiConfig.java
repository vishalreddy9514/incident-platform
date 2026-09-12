package com.incidentplatform.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Basic metadata for the generated OpenAPI spec (served at /v3/api-docs, browsable UI at
 * /swagger-ui.html). Per Phase 1 §9.1, this generated spec is the authoritative API contract —
 * {@code docs/api.md} links here rather than duplicating endpoint documentation by hand.
 */
@Configuration
public class OpenApiConfig {

  @Bean
  public OpenAPI incidentPlatformOpenApi() {
    return new OpenAPI()
        .info(
            new Info()
                .title("Incident Platform API")
                .description(
                    "REST API for the Cloud-Native AI Incident & Support Management Platform")
                .version("v1"));
  }
}
