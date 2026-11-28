package com.fourguard.wms.application.usecase;

import com.fourguard.wms.domain.exception.EntityNotFoundException;
import com.fourguard.wms.domain.ports.out.UserRepositoryPort;
import com.fourguard.wms.infrastructure.persistence.entity.UserEntity;
import com.fourguard.wms.shared.audit.SecurityAuditHelper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ResetUserPasswordUseCaseImplTest {

    @Mock
    private UserRepositoryPort userRepositoryPort;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private com.fourguard.wms.shared.audit.AuditService auditService;

    private ResetUserPasswordUseCaseImpl resetUserPasswordUseCase;

    private UserEntity userEntity;
    private UserEntity adminUserEntity;
    private UUID userId;

    @BeforeEach
    void setUp() {
        resetUserPasswordUseCase = new ResetUserPasswordUseCaseImpl(
                userRepositoryPort, passwordEncoder, auditService, new SecurityAuditHelper());

        userId = UUID.randomUUID();
        userEntity = UserEntity.builder()
                .id(userId)
                .username("testoperator")
                .email("operator@4guard.mx")
                .password("oldHashedPassword")
                .changePasswordRequired(false)
                .build();
        adminUserEntity = UserEntity.builder()
                .id(UUID.randomUUID())
                .username("adminUser")
                .email("admin@4guard.mx")
                .build();
    }

    @Test
    void whenResetToTemporaryPassword_byId_thenSuccess() {
        // Arrange
        when(userRepositoryPort.findById(userId)).thenReturn(Optional.of(userEntity));
        when(userRepositoryPort.findByUsername("adminUser")).thenReturn(Optional.of(adminUserEntity));
        when(passwordEncoder.encode(anyString())).thenReturn("newHashedPassword");

        // Act
        String tempPassword = resetUserPasswordUseCase.resetToTemporaryPasswordById(userId, "adminUser");

        // Assert
        assertNotNull(tempPassword);
        assertTrue(tempPassword.startsWith("4G-"));
        assertTrue(tempPassword.endsWith("*"));

        ArgumentCaptor<UserEntity> captor = ArgumentCaptor.forClass(UserEntity.class);
        verify(userRepositoryPort, times(1)).save(captor.capture());

        UserEntity savedUser = captor.getValue();
        assertEquals("newHashedPassword", savedUser.getPassword());
        assertTrue(savedUser.getChangePasswordRequired());
        assertEquals("adminUser", savedUser.getUpdatedBy());
    }

    @Test
    void whenResetToTemporaryPassword_byUsername_thenSuccess() {
        // Arrange
        when(userRepositoryPort.findByUsernameOrEmail("testoperator")).thenReturn(Optional.of(userEntity));
        when(userRepositoryPort.findByUsername("adminUser")).thenReturn(Optional.of(adminUserEntity));
        when(passwordEncoder.encode(anyString())).thenReturn("newHashedPassword");

        // Act
        String tempPassword = resetUserPasswordUseCase.resetToTemporaryPassword("testoperator", "adminUser");

        // Assert
        assertNotNull(tempPassword);
        assertTrue(tempPassword.startsWith("4G-"));
        assertTrue(tempPassword.endsWith("*"));

        ArgumentCaptor<UserEntity> captor = ArgumentCaptor.forClass(UserEntity.class);
        verify(userRepositoryPort, times(1)).save(captor.capture());

        UserEntity savedUser = captor.getValue();
        assertEquals("newHashedPassword", savedUser.getPassword());
        assertTrue(savedUser.getChangePasswordRequired());
        assertEquals("adminUser", savedUser.getUpdatedBy());
    }

    @Test
    void whenResetToTemporaryPassword_byEmail_thenSuccess() {
        // Arrange
        when(userRepositoryPort.findByUsernameOrEmail("operator@4guard.mx")).thenReturn(Optional.of(userEntity));
        when(userRepositoryPort.findByUsername("adminUser")).thenReturn(Optional.of(adminUserEntity));
        when(passwordEncoder.encode(anyString())).thenReturn("newHashedPassword");

        // Act
        String tempPassword = resetUserPasswordUseCase.resetToTemporaryPassword("operator@4guard.mx", "adminUser");

        // Assert
        assertNotNull(tempPassword);
        assertTrue(tempPassword.startsWith("4G-"));
        verify(userRepositoryPort, times(1)).save(any(UserEntity.class));
    }

    @Test
    void whenResetToTemporaryPassword_withSystemActor_thenSuccess() {
        // Arrange
        when(userRepositoryPort.findByUsernameOrEmail("testoperator")).thenReturn(Optional.of(userEntity));
        when(passwordEncoder.encode(anyString())).thenReturn("newHashedPassword");

        // Act
        String tempPassword = resetUserPasswordUseCase.resetToTemporaryPassword("testoperator", "SYSTEM");

        // Assert
        assertNotNull(tempPassword);
        verify(userRepositoryPort, never()).findByUsername("SYSTEM");
        verify(userRepositoryPort, times(1)).save(any(UserEntity.class));
    }

    @Test
    void whenResetToTemporaryPassword_andUserNotFound_thenThrowException() {
        // Arrange — ningún usuario ni email coincide
        when(userRepositoryPort.findByUsernameOrEmail("noexiste")).thenReturn(Optional.empty());

        // Act & Assert
        EntityNotFoundException ex = assertThrows(EntityNotFoundException.class, () ->
                resetUserPasswordUseCase.resetToTemporaryPassword("noexiste", "adminUser"));

        assertTrue(ex.getMessage().contains("noexiste"),
                "El mensaje de error debe incluir el identificador proporcionado");

        verify(userRepositoryPort, never()).save(any());
    }
}
