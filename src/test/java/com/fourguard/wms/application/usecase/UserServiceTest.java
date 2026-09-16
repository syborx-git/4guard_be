package com.fourguard.wms.application.usecase;

import com.fourguard.wms.application.dto.UserCreateRequest;
import com.fourguard.wms.application.dto.UserResponse;
import com.fourguard.wms.application.dto.UserUpdateRequest;
import com.fourguard.wms.application.dto.response.audit.UserAuditResponse;
import com.fourguard.wms.application.mapper.UserMapper;
import com.fourguard.wms.domain.enums.UserStatus;
import com.fourguard.wms.domain.model.User;
import com.fourguard.wms.domain.ports.out.AuditLogRepositoryPort;
import com.fourguard.wms.domain.ports.out.BranchRepositoryPort;
import com.fourguard.wms.domain.ports.out.OrganizationRepositoryPort;
import com.fourguard.wms.domain.ports.out.RoleRepositoryPort;
import com.fourguard.wms.domain.ports.out.UserRepositoryPort;
import com.fourguard.wms.infrastructure.persistence.entity.AuditLogEntity;
import com.fourguard.wms.infrastructure.persistence.entity.BranchEntity;
import com.fourguard.wms.infrastructure.persistence.entity.OrganizationEntity;
import com.fourguard.wms.infrastructure.persistence.entity.RoleEntity;
import com.fourguard.wms.infrastructure.persistence.entity.UserEntity;
import com.fourguard.wms.domain.exception.EntityNotFoundException;
import com.fourguard.wms.domain.exception.ValidationException;
import com.fourguard.wms.shared.audit.AuditService;
import com.fourguard.wms.shared.audit.SecurityAuditHelper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepositoryPort userRepositoryPort;
    @Mock
    private OrganizationRepositoryPort organizationRepositoryPort;
    @Mock
    private BranchRepositoryPort branchRepositoryPort;
    @Mock
    private RoleRepositoryPort roleRepositoryPort;
    @Mock
    private AuditLogRepositoryPort auditLogRepositoryPort;
    @Mock
    private UserMapper userMapper;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private SecurityAuditHelper securityAuditHelper;
    @Mock
    private AuditService auditService;

    @InjectMocks
    private UserService userService;

    private UUID userId;
    private UUID orgId;
    private UUID branchId;
    private UUID roleId;

    private UserCreateRequest createRequest;
    private UserUpdateRequest updateRequest;
    private UserResponse userResponse;
    private User user;
    private UserEntity userEntity;

    private OrganizationEntity orgEntity;
    private BranchEntity branchEntity;
    private RoleEntity roleEntity;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        orgId = UUID.randomUUID();
        branchId = UUID.randomUUID();
        roleId = UUID.randomUUID();

        createRequest = UserCreateRequest.builder()
                .username("john.doe")
                .email("john.doe@4guard.com")
                .password("securePassword123")
                .firstName("John")
                .lastName("Doe")
                .organizationId(orgId)
                .branchId(branchId)
                .roleId(roleId)
                .status(UserStatus.ACTIVE)
                .isEnabled(true)
                .build();

        updateRequest = UserUpdateRequest.builder()
                .id(userId)
                .username("john.updated")
                .email("john.updated@4guard.com")
                .password("newSecurePassword")
                .firstName("John")
                .lastName("Doe")
                .organizationId(orgId)
                .branchId(branchId)
                .roleId(roleId)
                .status(UserStatus.ACTIVE)
                .isEnabled(true)
                .build();

        user = User.builder()
                .id(userId)
                .username("john.doe")
                .email("john.doe@4guard.com")
                .firstName("John")
                .lastName("Doe")
                .build();

        userEntity = UserEntity.builder()
                .id(userId)
                .username("john.doe")
                .email("john.doe@4guard.com")
                .firstName("John")
                .lastName("Doe")
                .build();

        userResponse = UserResponse.builder()
                .id(userId)
                .username("john.doe")
                .email("john.doe@4guard.com")
                .firstName("John")
                .lastName("Doe")
                .organizationId(orgId)
                .branchId(branchId)
                .roleId(roleId)
                .build();

        orgEntity = OrganizationEntity.builder().id(orgId).name("Test Org").build();
        branchEntity = BranchEntity.builder().id(branchId).name("Test Branch").build();
        roleEntity = RoleEntity.builder().id(roleId).name("Test Role").build();
    }

    @Test
    void whenCreateUser_withValidData_thenSuccess() {
        // Arrange
        when(userRepositoryPort.existsByUsername(createRequest.getUsername())).thenReturn(false);
        when(userRepositoryPort.existsByEmail(createRequest.getEmail())).thenReturn(false);
        when(userMapper.toUser(createRequest)).thenReturn(user);

        when(organizationRepositoryPort.findById(orgId)).thenReturn(Optional.of(orgEntity));
        when(branchRepositoryPort.findById(branchId)).thenReturn(Optional.of(branchEntity));
        when(roleRepositoryPort.findById(roleId)).thenReturn(Optional.of(roleEntity));

        when(passwordEncoder.encode(createRequest.getPassword())).thenReturn("hashedPassword");
        when(userMapper.toUserEntity(any(User.class))).thenReturn(userEntity);
        when(userRepositoryPort.save(any(UserEntity.class))).thenReturn(userEntity);
        when(userMapper.toUser(any(UserEntity.class))).thenReturn(user);
        when(userMapper.toUserResponse(any(User.class))).thenReturn(userResponse);

        when(securityAuditHelper.getCurrentUsername()).thenReturn("test-admin");

        // Act
        UserResponse response = userService.createUser(createRequest);

        // Assert
        assertNotNull(response);
        assertEquals(userId, response.getId());
        assertEquals("john.doe", response.getUsername());
        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userMapper, times(1)).toUserEntity(userCaptor.capture());
        assertTrue(userCaptor.getValue().getChangePasswordRequired());
        assertEquals(0, userCaptor.getValue().getFailedAttempts());
        assertFalse(userCaptor.getValue().getPermanentlyLocked());
        assertEquals("test-admin", userCaptor.getValue().getCreatedBy());
        verify(userRepositoryPort, times(1)).save(any(UserEntity.class));
    }

    @Test
    void whenCreateUser_withDuplicateUsername_thenThrowsValidationException() {
        // Arrange
        when(userRepositoryPort.existsByUsername(createRequest.getUsername())).thenReturn(true);

        // Act & Assert
        assertThrows(ValidationException.class, () -> userService.createUser(createRequest));
        verify(userRepositoryPort, never()).save(any());
    }

    @Test
    void whenCreateUser_withDuplicateEmail_thenThrowsValidationException() {
        // Arrange
        when(userRepositoryPort.existsByUsername(createRequest.getUsername())).thenReturn(false);
        when(userRepositoryPort.existsByEmail(createRequest.getEmail())).thenReturn(true);

        // Act & Assert
        assertThrows(ValidationException.class, () -> userService.createUser(createRequest));
        verify(userRepositoryPort, never()).save(any());
    }

    @Test
    void whenGetAllUsers_thenReturnList() {
        // Arrange
        when(userRepositoryPort.findAll()).thenReturn(List.of(userEntity));
        when(userMapper.toUserList(anyList())).thenReturn(List.of(user));
        when(userMapper.toUserResponseList(anyList())).thenReturn(List.of(userResponse));

        // Act
        List<UserResponse> list = userService.getAllUsers();

        // Assert
        assertNotNull(list);
        assertEquals(1, list.size());
        verify(userRepositoryPort, times(1)).findAll();
    }

    @Test
    void whenGetUserById_withExistingId_thenReturnUser() {
        // Arrange
        when(userRepositoryPort.findById(userId)).thenReturn(Optional.of(userEntity));
        when(userMapper.toUser(userEntity)).thenReturn(user);
        when(userMapper.toUserResponse(user)).thenReturn(userResponse);

        // Act
        UserResponse response = userService.getUserById(userId);

        // Assert
        assertNotNull(response);
        assertEquals(userId, response.getId());
        verify(userRepositoryPort, times(1)).findById(userId);
    }

    @Test
    void whenGetUserById_withNonExistingId_thenThrowsEntityNotFoundException() {
        // Arrange
        when(userRepositoryPort.findById(userId)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(EntityNotFoundException.class, () -> userService.getUserById(userId));
    }

    @Test
    void whenUpdateUser_withValidData_thenSuccess() {
        // Arrange
        when(userRepositoryPort.findById(userId)).thenReturn(Optional.of(userEntity));
        when(userRepositoryPort.existsByUsername(updateRequest.getUsername())).thenReturn(false);
        when(userRepositoryPort.existsByEmail(updateRequest.getEmail())).thenReturn(false);

        when(organizationRepositoryPort.findById(orgId)).thenReturn(Optional.of(orgEntity));
        when(branchRepositoryPort.findById(branchId)).thenReturn(Optional.of(branchEntity));
        when(roleRepositoryPort.findById(roleId)).thenReturn(Optional.of(roleEntity));

        when(passwordEncoder.encode(updateRequest.getPassword())).thenReturn("newHashedPassword");
        when(userMapper.toUserEntity(any(User.class))).thenReturn(userEntity);
        when(userRepositoryPort.save(any(UserEntity.class))).thenReturn(userEntity);
        when(userMapper.toUser(any(UserEntity.class))).thenReturn(user);

        UserResponse updatedResponse = UserResponse.builder()
                .id(userId)
                .username("john.updated")
                .email("john.updated@4guard.com")
                .build();
        when(userMapper.toUserResponse(any(User.class))).thenReturn(updatedResponse);

        when(securityAuditHelper.getCurrentUsername()).thenReturn("test-admin");

        // Act
        UserResponse response = userService.updateUser(updateRequest);

        // Assert
        assertNotNull(response);
        assertEquals("john.updated", response.getUsername());
        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userMapper, times(1)).toUserEntity(userCaptor.capture());
        assertEquals("test-admin", userCaptor.getValue().getUpdatedBy());
        verify(userRepositoryPort, times(1)).save(any(UserEntity.class));
    }

    @Test
    void whenDeleteUser_withExistingId_thenSuccess() {
        // Arrange
        when(userRepositoryPort.findById(userId)).thenReturn(Optional.of(userEntity));
        when(securityAuditHelper.getCurrentUsername()).thenReturn("test-admin");

        // Act
        userService.deleteUser(userId);

        // Assert
        verify(userRepositoryPort, times(1)).deleteById(userId);
    }

    @Test
    void whenDeleteUser_withNonExistingId_thenThrowsEntityNotFoundException() {
        // Arrange
        when(userRepositoryPort.findById(userId)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(EntityNotFoundException.class, () -> userService.deleteUser(userId));
        verify(userRepositoryPort, never()).deleteById(any());
    }

    @Test
    void whenGetUserAuditLogs_withExistingId_thenReturnLogs() {
        // Arrange
        when(userRepositoryPort.findById(userId)).thenReturn(Optional.of(userEntity));
        AuditLogEntity logEntity = AuditLogEntity.builder()
                .logId(UUID.randomUUID())
                .action("USER_CREATED")
                .userId(userId)
                .details(List.of())
                .build();
        when(auditLogRepositoryPort.findByEntityTypeAndEntityId("USER", userId)).thenReturn(List.of(logEntity));

        // Act
        List<UserAuditResponse> logs = userService.getUserAuditLogs(userId);

        // Assert
        assertNotNull(logs);
        assertEquals(1, logs.size());
        assertEquals("USER_CREATED", logs.get(0).getAction());
        verify(auditLogRepositoryPort, times(1)).findByEntityTypeAndEntityId("USER", userId);
    }
}
