package com.incidentplatform.security;

import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Reads a {@code Bearer} token from the {@code Authorization} header, validates it via {@link
 * JwtService}, and — if valid — loads the referenced user via {@link CustomUserDetailsService}
 * and populates the {@link SecurityContextHolder} so downstream {@code @PreAuthorize} checks and
 * {@code anyRequest().authenticated()} work.
 *
 * <p>Any failure here (missing header, malformed/expired token, unknown user, deactivated
 * account) is treated as "request stays unauthenticated" rather than an error — the request
 * proceeds down the filter chain and Spring Security's own authorization rules then correctly
 * reject it with 401/403 via {@link RestAuthenticationEntryPoint}/{@link RestAccessDeniedHandler}.
 */
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

  private static final Logger log = LoggerFactory.getLogger(JwtAuthenticationFilter.class);
  private static final String BEARER_PREFIX = "Bearer ";

  private final JwtService jwtService;
  private final CustomUserDetailsService userDetailsService;

  public JwtAuthenticationFilter(
      JwtService jwtService, CustomUserDetailsService userDetailsService) {
    this.jwtService = jwtService;
    this.userDetailsService = userDetailsService;
  }

  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
      throws ServletException, IOException {
    String header = request.getHeader("Authorization");

    if (header != null && header.startsWith(BEARER_PREFIX)) {
      String token = header.substring(BEARER_PREFIX.length());
      try {
        String email = jwtService.parseClaims(token).get("email", String.class);
        if (email != null && SecurityContextHolder.getContext().getAuthentication() == null) {
          UserDetails userDetails = userDetailsService.loadUserByUsername(email);
          if (userDetails.isEnabled()) {
            var authToken =
                new UsernamePasswordAuthenticationToken(
                    userDetails, null, userDetails.getAuthorities());
            authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
            SecurityContextHolder.getContext().setAuthentication(authToken);
          }
        }
      } catch (JwtException | IllegalArgumentException ex) {
        log.debug("Rejected invalid JWT: {}", ex.getMessage());
      } catch (UsernameNotFoundException ex) {
        log.debug("JWT referenced a user that no longer exists: {}", ex.getMessage());
      }
    }

    filterChain.doFilter(request, response);
  }
}
