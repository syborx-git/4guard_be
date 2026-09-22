package com.fourguard.wms.infrastructure.configuration;

import com.fourguard.wms.infrastructure.security.jwt.JwtAuthenticationFilter;
import com.fourguard.wms.shared.constants.SecurityConstants;
import lombok.RequiredArgsConstructor;
import java.util.List;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

/**
 * Complete Security Configuration — JWT Secured.
 *
 * <p>Restricts all endpoints except the auth whitelist.
 * Registers JwtAuthenticationFilter.
 * Enables method-level security ({@code @PreAuthorize}).</p>
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthFilter;
    private final UserDetailsService userDetailsService;
    private final PasswordEncoder passwordEncoder;
    private final CorsProperties corsProperties;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .csrf(AbstractHttpConfigurer::disable) // Temporarily disabled for debugging
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        .requestMatchers(
                                SecurityConstants.AUTH_PATTERN,
                                SecurityConstants.RESET_PASSWORD_TEMP_PATTERN,
                                SecurityConstants.ACTUATOR_HEALTH,
                                SecurityConstants.SECURITY_GATE_PUBLIC_PATTERN,
                                "/swagger-ui.html",
                                "/swagger-ui/**",
                                "/v3/api-docs/**",
                                "/v3/api-docs.yaml",
                                "/webjars/**"
                        ).permitAll()
                        .anyRequest().authenticated()
                )
                .authenticationProvider(authenticationProvider())
                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public AuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider();
        authProvider.setUserDetailsService(userDetailsService);
        authProvider.setPasswordEncoder(passwordEncoder);
        return authProvider;
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();

        List<String> origins = corsProperties.getAllowedOrigins();
        if (origins != null && !origins.isEmpty()) {
            config.setAllowedOrigins(origins);
        } else {
            config.setAllowedOrigins(List.of(
                "http://localhost:4200",
                "http://localhost:4201",
                "http://localhost:8080",
                "http://127.0.0.1:4200",
                "http://127.0.0.1:4201"
            ));
        }

        List<String> patterns = corsProperties.getAllowedOriginPatterns();
        if (patterns != null && !patterns.isEmpty()) {
            config.setAllowedOriginPatterns(patterns);
        } else {
            config.setAllowedOriginPatterns(List.of(
                "http://localhost:*",
                "http://127.0.0.1:*",
                "https://*.ngrok-free.dev",
                "https://*.onrender.com"
            ));
        }

        config.setAllowedMethods(corsProperties.getAllowedMethods() != null && !corsProperties.getAllowedMethods().isEmpty()
                ? corsProperties.getAllowedMethods()
                : List.of("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH"));

        config.setAllowedHeaders(corsProperties.getAllowedHeaders() != null && !corsProperties.getAllowedHeaders().isEmpty()
                ? corsProperties.getAllowedHeaders()
                : List.of("*"));

        config.setAllowCredentials(corsProperties.isAllowCredentials());
        config.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }
}