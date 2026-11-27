package com.fourguard.wms.infrastructure.security.jwt;

import com.fourguard.wms.domain.ports.out.TokenBlacklistPort;
import com.fourguard.wms.shared.constants.SecurityConstants;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.UnsupportedJwtException;
import io.jsonwebtoken.security.SignatureException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.UUID;

/**
 * Filter that intercepts HTTP requests, extracts Bearer JWT from headers,
 * validates it, and sets the SecurityContext if valid.
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final UserDetailsService userDetailsService;
    private final TokenBlacklistPort tokenBlacklistPort;
    private final AntPathMatcher antPathMatcher = new AntPathMatcher();

    // List of paths that should not be filtered by this JWT filter
    private static final List<String> EXCLUDED_PATHS = Arrays.asList(
            SecurityConstants.AUTH_PATTERN,              // /auth/**
            SecurityConstants.SECURITY_GATE_PUBLIC_PATTERN, // /security-gate/public/**
            SecurityConstants.NOTIFICATIONS_RF_STREAM_PATTERN, // /notifications/rf-stream
            SecurityConstants.NOTIFICATIONS_RF_ALERTS_PATTERN, // /notifications/rf-alerts/**
            SecurityConstants.WEBSOCKET_ENDPOINT_PATTERN,      // /ws-wms/**
            SecurityConstants.ACTUATOR_HEALTH,           // /actuator/health
            "/swagger-ui.html",
            "/swagger-ui/**",
            "/v3/api-docs/**",
            "/v3/api-docs.yaml",
            "/webjars/**"
    );

    @Override
    protected boolean shouldNotFilter(@NonNull HttpServletRequest request) throws ServletException {
        // Get the path without the context path, as Spring Security's requestMatchers also operate on this path
        String path = request.getRequestURI().substring(request.getContextPath().length());
        if ("/auth/logout".equals(path)) {
            return false;
        }
        return EXCLUDED_PATHS.stream().anyMatch(pattern -> antPathMatcher.match(pattern, path));
    }

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain) throws ServletException, IOException {

        final String authHeader = request.getHeader(SecurityConstants.AUTHORIZATION_HEADER);
        final String jwt;
        final String username;

        // Verify if header exists and starts with "Bearer "
        if (authHeader == null || !authHeader.startsWith(SecurityConstants.BEARER_PREFIX)) {
            filterChain.doFilter(request, response);
            return;
        }

        try {
            jwt = authHeader.substring(SecurityConstants.BEARER_PREFIX.length());
            username = jwtService.extractUsername(jwt);
            UUID userId = jwtService.extractUserId(jwt);
            Date issuedAt = jwtService.extractIssuedAt(jwt);

            if (userId != null && tokenBlacklistPort.isUserRevoked(userId, issuedAt)) {
                log.warn("[Security] Request rejected for revoked user session: {}", username);
                filterChain.doFilter(request, response);
                return;
            }

            if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                UserDetails userDetails = this.userDetailsService.loadUserByUsername(username);
                if (jwtService.isTokenValid(jwt, userDetails.getUsername())) {
                    UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                            userDetails,
                            null,
                            userDetails.getAuthorities()
                    );
                    authToken.setDetails(
                            new WebAuthenticationDetailsSource().buildDetails(request)
                    );
                    SecurityContextHolder.getContext().setAuthentication(authToken);
                }
            }
        } catch (ExpiredJwtException e) {
            log.warn("JWT token has expired: {}", e.getMessage());
            writeErrorResponse(response, HttpServletResponse.SC_UNAUTHORIZED, "No autenticado o token expirado");
            return;
        } catch (SignatureException | MalformedJwtException | UnsupportedJwtException | IllegalArgumentException e) {
            log.warn("Invalid JWT token: {}", e.getMessage());
            writeErrorResponse(response, HttpServletResponse.SC_UNAUTHORIZED, "Token JWT inválido");
            return;
        } catch (org.springframework.security.core.AuthenticationException e) {
            log.warn("[Security] User authentication rejected in JWT filter: {}", e.getMessage());
            writeErrorResponse(response, HttpServletResponse.SC_UNAUTHORIZED, "Cuenta desactivada o sesión inválida");
            return;
        } catch (DataAccessException ex) {
            Throwable mostSpecificCause = ex.getMostSpecificCause();
            String sqlDetail = mostSpecificCause != null ? mostSpecificCause.getMessage() : ex.getMessage();
            log.error("[JWT-FILTER-DB-ERROR] Error SQL/DB validando sesión JWT para {}: {} | RootCause: {}",
                    request.getRequestURI(), ex.getMessage(), sqlDetail, ex);
            writeErrorResponse(response, HttpServletResponse.SC_SERVICE_UNAVAILABLE,
                    "Servicio temporalmente no disponible por error de base de datos");
            return;
        }

        filterChain.doFilter(request, response);
    }

    private void writeErrorResponse(HttpServletResponse response, int status, String message) throws IOException {
        response.setStatus(status);
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        response.getWriter().write("{\"success\":false,\"message\":\"" + message + "\",\"data\":null}");
    }
}