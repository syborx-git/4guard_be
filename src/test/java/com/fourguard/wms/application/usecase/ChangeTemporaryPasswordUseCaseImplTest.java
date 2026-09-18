package com.fourguard.wms.application.usecase;

import com.fourguard.wms.domain.exception.EntityNotFoundException;
import com.fourguard.wms.domain.ports.out.UserRepositoryPort;
import com.fourguard.wms.infrastructure.persistence.entity.UserEntity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ChangeTemporaryPasswordUseCaseImplTest {

    @Mock
    private UserRepositoryPort userRepositoryPort;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private com.fourguard.wms.shared.audit.AuditService auditService;

    @InjectMocks
    private ChangeTemporaryPasswordUseCaseImpl changeTemporaryPasswordUseCase;

    private UserEntity userEntity;
    private String username;

    @BeforeEach
    void setUp() {
        username = "operator123";
        userEntity = UserEntity.builder()
                .id(java.util.UUID.randomUUID())
                .username(username)
                .email("operator123@4guard.mx")
                .password("tempHashedPassword")
                .changePasswordRequired(true)
                .build();
    }

    @Test
    void whenChangePassword_thenSuccess() {
        // Arrange
        when(userRepositoryPort.findByUsername(username)).thenReturn(Optional.of(userEntity));
        when(passwordEncoder.encode("newPermanentPassword")).thenReturn("newHashedPassword");

        // Act
        changeTemporaryPasswordUseCase.changePassword(username, "newPermanentPassword");

        // Assert
        ArgumentCaptor<UserEntity> captor = ArgumentCaptor.forClass(UserEntity.class);
        verify(userRepositoryPort, times(1)).save(captor.capture());

        UserEntity savedUser = captor.getValue();
        assertEquals("newHashedPassword", savedUser.getPassword());
        assertFalse(savedUser.getChangePasswordRequired());
        assertEquals(username, savedUser.getUpdatedBy());
    }

    @Test
    void whenChangePassword_andUserNotFound_thenThrowException() {
        // Arrange
        when(userRepositoryPort.findByUsername(username)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(EntityNotFoundException.class,
                () -> changeTemporaryPasswordUseCase.changePassword(username, "newPermanentPassword"));

        verify(userRepositoryPort, never()).save(any());
    }
}
