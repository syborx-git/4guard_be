package com.fourguard.wms.infrastructure.security.jwt;

import com.fourguard.wms.domain.ports.out.TokenBlacklistPort;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.InvalidDataAccessResourceUsageException;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetailsService;

import java.io.IOException;
import java.sql.SQLException;
import java.util.Date;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class JwtAuthenticationFilterTest {

    @Mock
    private JwtService jwtService;
    @Mock
    private UserDetailsService userDetailsService;
    @Mock
    private TokenBlacklistPort tokenBlacklistPort;

    private JwtAuthenticationFilter filter;

    @BeforeEach
    void setUp() {
        filter = new JwtAuthenticationFilter(jwtService, userDetailsService, tokenBlacklistPort);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void shouldNotFilterWebSocketHandshakePathsEvenWithAuthorizationHeader() throws ServletException {
        MockHttpServletRequest wsRoot = new MockHttpServletRequest("GET", "/api/v1/ws-wms");
        wsRoot.setContextPath("/api/v1");
        wsRoot.addHeader("Authorization", "Bearer expired-or-stale-token");

        MockHttpServletRequest sockJsInfo = new MockHttpServletRequest("GET", "/api/v1/ws-wms/info");
        sockJsInfo.setContextPath("/api/v1");
        sockJsInfo.addHeader("Authorization", "Bearer expired-or-stale-token");

        assertTrue(filter.shouldNotFilter(wsRoot));
        assertTrue(filter.shouldNotFilter(sockJsInfo));
    }

    @Test
    void whenDataAccessExceptionOccursInFilter_thenReturns503ClearJson() throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/chat/conversations");
        request.setContextPath("/api/v1");
        request.addHeader("Authorization", "Bearer valid-jwt-token");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = spy(new MockFilterChain());

        UUID userId = UUID.randomUUID();
        Date issuedAt = new Date();
        when(jwtService.extractUsername("valid-jwt-token")).thenReturn("admin");
        when(jwtService.extractUserId("valid-jwt-token")).thenReturn(userId);
        when(jwtService.extractIssuedAt("valid-jwt-token")).thenReturn(issuedAt);
        when(tokenBlacklistPort.isUserRevoked(userId, issuedAt)).thenReturn(false);

        SQLException sqlEx = new SQLException("ERROR: column u1_0.chat_frozen_until does not exist", "42703");
        when(userDetailsService.loadUserByUsername("admin"))
                .thenThrow(new InvalidDataAccessResourceUsageException("could not extract ResultSet", sqlEx));

        filter.doFilterInternal(request, response, chain);

        assertEquals(503, response.getStatus());
        assertTrue(response.getContentType().startsWith("application/json"));
        assertTrue(response.getContentAsString().contains("\"success\":false"));
        assertTrue(response.getContentAsString().contains("Servicio temporalmente no disponible"));
        verify(chain, never()).doFilter(any(), any());
    }
}
