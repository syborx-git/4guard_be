package com.fourguard.wms.infrastructure.security.jwt;

import com.fourguard.wms.shared.constants.SecurityConstants;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.util.AntPathMatcher;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import com.fourguard.wms.domain.ports.out.TokenBlacklistPort;

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
            SecurityConstants.RESET_PASSWORD_TEMP_PATTERN, // /users/reset-password-temp (public)
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
            java.util.UUID userId = jwtService.extractUserId(jwt);
            java.util.Date issuedAt = jwtService.extractIssuedAt(jwt);

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
        } catch (io.jsonwebtoken.ExpiredJwtException e) {
            log.warn("JWT token has expired: {}", e.getMessage());
            // No seteamos el contexto de seguridad. La cadena continuará y Spring Security
            // bloqueará los endpoints protegidos retornando un 401 Unauthorized limpio en vez de un 500.
        } catch (io.jsonwebtoken.security.SignatureException | io.jsonwebtoken.MalformedJwtException | io.jsonwebtoken.UnsupportedJwtException | IllegalArgumentException e) {
            log.warn("Invalid JWT token: {}", e.getMessage());
        }

        filterChain.doFilter(request, response);
    }
}