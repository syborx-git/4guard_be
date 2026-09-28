package com.fourguard.wms.application.usecase;

import com.fourguard.wms.application.dto.request.auth.LogoutRequest;
import com.fourguard.wms.domain.exception.EntityNotFoundException;
import com.fourguard.wms.domain.exception.InvalidCredentialsException;
import com.fourguard.wms.domain.ports.out.TokenBlacklistPort;
import com.fourguard.wms.domain.ports.out.UserRepositoryPort;
import com.fourguard.wms.infrastructure.persistence.entity.UserEntity;
import com.fourguard.wms.infrastructure.security.jwt.JwtService;
import com.fourguard.wms.shared.audit.AuditService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LogoutUseCaseImplTest {

    @Mock
    private UserRepositoryPort userRepositoryPort;

    @Mock
    private JwtService jwtService;

    @Mock
    private AuditService auditService;

    @Mock
    private TokenBlacklistPort tokenBlacklistPort;

    @InjectMocks
    private LogoutUseCaseImpl logoutUseCase;

    private String username;
    private String token;
    private LogoutRequest logoutRequest;
    private UserEntity userEntity;

    @BeforeEach
    void setUp() {
        username = "operator123";
        token = "validRefreshToken";
        logoutRequest = LogoutRequest.builder().refreshToken(token).build();
        userEntity = UserEntity.builder()
                .id(java.util.UUID.randomUUID())
                .username(username)
                .build();
    }

    @Test
    void whenLogout_thenSuccess() {
        // Arrange
        when(jwtService.extractUsername(token)).thenReturn(username);
        when(jwtService.isTokenExpired(token)).thenReturn(false);
        when(userRepositoryPort.findByUsername(username)).thenReturn(Optional.of(userEntity));

        // Act
        logoutUseCase.logout(logoutRequest, username);

        // Assert
        verify(userRepositoryPort, times(1)).findByUsername(username);
        verify(tokenBlacklistPort, times(1)).revokeUserSessions(userEntity.getId());
        verify(auditService, times(1)).log(eq(userEntity), eq("LOGOUT"), eq("USER"), eq(userEntity.getId()), any(), any());
    }

    @Test
    void whenLogout_withMismatchedTokenUser_thenThrowException() {
        // Arrange
        when(jwtService.extractUsername(token)).thenReturn("differentUser");

        // Act & Assert
        assertThrows(InvalidCredentialsException.class, () ->
                logoutUseCase.logout(logoutRequest, username));

        verify(userRepositoryPort, never()).findByUsername(anyString());
        verify(tokenBlacklistPort, never()).revokeUserSessions(any());
    }

    @Test
    void whenLogout_withExpiredToken_thenThrowException() {
        // Arrange
        when(jwtService.extractUsername(token)).thenReturn(username);
        when(jwtService.isTokenExpired(token)).thenReturn(true);

        // Act & Assert
        assertThrows(InvalidCredentialsException.class, () ->
                logoutUseCase.logout(logoutRequest, username));

        verify(userRepositoryPort, never()).findByUsername(anyString());
        verify(tokenBlacklistPort, never()).revokeUserSessions(any());
    }

    @Test
    void whenLogout_andUserNotFound_thenThrowException() {
        // Arrange
        when(jwtService.extractUsername(token)).thenReturn(username);
        when(jwtService.isTokenExpired(token)).thenReturn(false);
        when(userRepositoryPort.findByUsername(username)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(EntityNotFoundException.class, () ->
                logoutUseCase.logout(logoutRequest, username));

        verify(tokenBlacklistPort, never()).revokeUserSessions(any());
    }
}
