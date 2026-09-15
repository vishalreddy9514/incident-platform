package com.incidentplatform.config;

import java.time.Duration;
import org.springframework.boot.web.client.RestClientCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;

/**
 * Applies sane connect/read timeouts to every {@code RestClient.Builder} injected anywhere in the
 * app (currently just {@code AiAnalysisClient}) — kept here rather than in the client itself so the
 * client's own constructor stays free to accept a builder a test has already bound to {@code
 * MockRestServiceServer} without this overwriting its request factory.
 */
@Configuration
public class RestClientConfig {

  private static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(5);
  private static final Duration READ_TIMEOUT = Duration.ofSeconds(15);

  @Bean
  public RestClientCustomizer timeoutRestClientCustomizer() {
    return builder -> {
      SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
      requestFactory.setConnectTimeout(CONNECT_TIMEOUT);
      requestFactory.setReadTimeout(READ_TIMEOUT);
      builder.requestFactory(requestFactory);
    };
  }
}
