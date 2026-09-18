package com.incidentplatform.config;

import com.incidentplatform.observability.RequestCorrelationFilter;
import com.incidentplatform.security.JwtAuthenticationFilter;
import com.incidentplatform.security.RateLimitingFilter;
import com.incidentplatform.security.RestAccessDeniedHandler;
import com.incidentplatform.security.RestAuthenticationEntryPoint;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

/**
 * Real authentication/authorisation config, replacing Phase 4's temporary permit-all. Every
 * endpoint requires a valid JWT except the auth endpoints themselves and health/docs; role-based
 * rules beyond that are applied per-endpoint via {@code @PreAuthorize} (ADR-0002), not here.
 *
 * <p>No {@code AuthenticationManager} bean — see ADR-0008 for why login doesn't use Spring
 * Security's standard authentication machinery.
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

  private final RequestCorrelationFilter requestCorrelationFilter;
  private final JwtAuthenticationFilter jwtAuthenticationFilter;
  private final RateLimitingFilter rateLimitingFilter;
  private final RestAuthenticationEntryPoint authenticationEntryPoint;
  private final RestAccessDeniedHandler accessDeniedHandler;

  public SecurityConfig(
      RequestCorrelationFilter requestCorrelationFilter,
      JwtAuthenticationFilter jwtAuthenticationFilter,
      RateLimitingFilter rateLimitingFilter,
      RestAuthenticationEntryPoint authenticationEntryPoint,
      RestAccessDeniedHandler accessDeniedHandler) {
    this.requestCorrelationFilter = requestCorrelationFilter;
    this.jwtAuthenticationFilter = jwtAuthenticationFilter;
    this.rateLimitingFilter = rateLimitingFilter;
    this.authenticationEntryPoint = authenticationEntryPoint;
    this.accessDeniedHandler = accessDeniedHandler;
  }

  @Bean
  public PasswordEncoder passwordEncoder() {
    return new BCryptPasswordEncoder();
  }

  @Bean
  public CorsConfigurationSource corsConfigurationSource(
      @Value("${app.cors.allowed-origin}") String allowedOrigin) {
    CorsConfiguration configuration = new CorsConfiguration();
    configuration.setAllowedOrigins(List.of(allowedOrigin));
    configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
    configuration.setAllowedHeaders(List.of("*"));
    configuration.setAllowCredentials(true);
    UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
    source.registerCorsConfiguration("/**", configuration);
    return source;
  }

  @Bean
  public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
    http.csrf(AbstractHttpConfigurer::disable)
        .cors(Customizer.withDefaults())
        .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
        .authorizeHttpRequests(
            auth ->
                auth.requestMatchers("/api/v1/auth/**")
                    .permitAll()
                    // /actuator/prometheus is unauthenticated so Prometheus can scrape it - it
                    // carries no secrets, only counters/histograms, and (docker-compose.yml,
                    // modules/networking's security groups) it's never reachable from outside
                    // the backend's own network in the first place.
                    .requestMatchers("/actuator/health", "/actuator/info", "/actuator/prometheus")
                    .permitAll()
                    .requestMatchers("/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html")
                    .permitAll()
                    .anyRequest()
                    .authenticated())
        .exceptionHandling(
            ex ->
                ex.authenticationEntryPoint(authenticationEntryPoint)
                    .accessDeniedHandler(accessDeniedHandler))
        // Spring Security's own header defaults already cover X-Content-Type-Options,
        // X-Frame-Options, Cache-Control, and (when the request is actually HTTPS) HSTS - these
        // two are the ones without a sane built-in default: an API response has no reason to leak
        // the requesting page's full URL to whatever it links to, and the JSON API this backend
        // serves has no use for any browser feature Permissions-Policy can gate (Phase 15).
        .headers(
            headers ->
                headers
                    .referrerPolicy(
                        rp ->
                            rp.policy(
                                ReferrerPolicyHeaderWriter.ReferrerPolicy
                                    .STRICT_ORIGIN_WHEN_CROSS_ORIGIN))
                    .permissionsPolicy(
                        pp ->
                            pp.policy(
                                "camera=(), microphone=(), geolocation=(), payment=(), usb=()")))
        .addFilterBefore(rateLimitingFilter, UsernamePasswordAuthenticationFilter.class)
        .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
        .addFilterBefore(requestCorrelationFilter, RateLimitingFilter.class);
    return http.build();
  }
}
