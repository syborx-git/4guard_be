package com.fourguard.wms.application.usecase;

import com.fourguard.wms.application.dto.UserCreateRequest;
import com.fourguard.wms.application.dto.UserResponse;
import com.fourguard.wms.application.dto.UserUpdateRequest;
import com.fourguard.wms.application.dto.response.audit.UserAuditResponse;
import com.fourguard.wms.application.mapper.UserMapper;
import com.fourguard.wms.domain.enums.UserStatus;
import com.fourguard.wms.domain.model.Branch;
import com.fourguard.wms.domain.model.Organization;
import com.fourguard.wms.domain.model.Role;
import com.fourguard.wms.domain.model.User;
import com.fourguard.wms.domain.ports.in.CreateUserUseCase;
import com.fourguard.wms.domain.ports.in.GetUserUseCase;
import com.fourguard.wms.domain.ports.in.UpdateUserUseCase;
import com.fourguard.wms.domain.ports.in.DeleteUserUseCase;
import com.fourguard.wms.domain.ports.out.AuditLogRepositoryPort;
import com.fourguard.wms.domain.ports.out.BranchRepositoryPort;
import com.fourguard.wms.domain.ports.out.OrganizationRepositoryPort;
import com.fourguard.wms.domain.ports.out.RoleRepositoryPort;
import com.fourguard.wms.domain.ports.out.UserRepositoryPort;
import com.fourguard.wms.infrastructure.persistence.entity.AuditLogEntity;
import com.fourguard.wms.infrastructure.persistence.entity.UserEntity;
import com.fourguard.wms.domain.exception.EntityNotFoundException;
import com.fourguard.wms.domain.exception.ValidationException;
import com.fourguard.wms.shared.audit.AuditService;
import com.fourguard.wms.shared.audit.SecurityAuditHelper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Slf4j
public class UserService implements CreateUserUseCase, GetUserUseCase, UpdateUserUseCase, DeleteUserUseCase {

    private final UserRepositoryPort userRepositoryPort;
    private final OrganizationRepositoryPort organizationRepositoryPort;
    private final BranchRepositoryPort branchRepositoryPort;
    private final RoleRepositoryPort roleRepositoryPort;
    private final AuditLogRepositoryPort auditLogRepositoryPort;
    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final SecurityAuditHelper securityAuditHelper;
    private final AuditService auditService;

    // Usar @Lazy en el constructor para romper ciclos de dependencia
    public UserService(
            UserRepositoryPort userRepositoryPort,
            OrganizationRepositoryPort organizationRepositoryPort,
            BranchRepositoryPort branchRepositoryPort,
            RoleRepositoryPort roleRepositoryPort,
            AuditLogRepositoryPort auditLogRepositoryPort,
            @Lazy UserMapper userMapper,
            PasswordEncoder passwordEncoder,
            SecurityAuditHelper securityAuditHelper,
            AuditService auditService
    ) {
        this.userRepositoryPort = userRepositoryPort;
        this.organizationRepositoryPort = organizationRepositoryPort;
        this.branchRepositoryPort = branchRepositoryPort;
        this.roleRepositoryPort = roleRepositoryPort;
        this.auditLogRepositoryPort = auditLogRepositoryPort;
        this.userMapper = userMapper;
        this.passwordEncoder = passwordEncoder;
        this.securityAuditHelper = securityAuditHelper;
        this.auditService = auditService;
    }

    @Transactional
    public UserResponse createUser(UserCreateRequest request) {
        log.info("Creating user with username: {}", request.getUsername());

        if (userRepositoryPort.existsByUsername(request.getUsername())) {
            throw new ValidationException("Username already exists: " + request.getUsername());
        }
        if (userRepositoryPort.existsByEmail(request.getEmail())) {
            throw new ValidationException("Email already exists: " + request.getEmail());
        }

        User user = userMapper.toUser(request);

        // Resolve relationships
        user.setOrganization(findOrganizationById(request.getOrganizationId()));
        user.setBranch(findBranchById(request.getBranchId()));
        user.setRole(findRoleById(request.getRoleId()));

        // Set defaults
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setStatus(request.getStatus() != null ? request.getStatus() : UserStatus.PENDING);
        user.setIsEnabled(request.getIsEnabled() != null ? request.getIsEnabled() : true);
        user.setChangePasswordRequired(true);
        user.setFailedAttempts(0);
        user.setPermanentlyLocked(false);
        user.setCreatedAt(OffsetDateTime.now());

        String currentUser = securityAuditHelper.getCurrentUsername();
        user.setCreatedBy(currentUser);

        UserEntity savedUserEntity = userRepositoryPort.save(userMapper.toUserEntity(user));

        // Audit log
        logAuditChange(currentUser, "USER_CREATED", savedUserEntity.getId(), null, savedUserEntity);

        return userMapper.toUserResponse(userMapper.toUser(savedUserEntity));
    }

    @Transactional(readOnly = true)
    public List<UserResponse> getAllUsers() {
        log.debug("Fetching all users");
        List<UserEntity> userEntities = userRepositoryPort.findAll();
        List<User> users = userMapper.toUserList(userEntities);
        // Manually resolve relationships for each user in the list
        users.forEach(this::resolveUserRelationships);
        return userMapper.toUserResponseList(users);
    }

    @Transactional(readOnly = true)
    public UserResponse getUserById(UUID id) {
        log.debug("Fetching user with ID: {}", id);
        UserEntity userEntity = userRepositoryPort.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("User not found with ID: " + id));
        User user = userMapper.toUser(userEntity);
        resolveUserRelationships(user); // Resolve relationships for the single user
        return userMapper.toUserResponse(user);
    }

    @Transactional
    public UserResponse updateUser(UserUpdateRequest request) {
        log.info("Updating user with ID: {}", request.getId());

        UserEntity existingUserEntity = userRepositoryPort.findById(request.getId())
                .orElseThrow(() -> new EntityNotFoundException("User not found with ID: " + request.getId()));
        
        // Take snapshot for audit
        UserEntity originalSnapshot = cloneUserEntity(existingUserEntity);

        User existingUser = userMapper.toUser(existingUserEntity);

        // Check for username/email uniqueness if they are being changed
        if (request.getUsername() != null && !request.getUsername().equals(existingUser.getUsername()) &&
            userRepositoryPort.existsByUsername(request.getUsername())) {
            throw new ValidationException("Username already exists: " + request.getUsername());
        }
        if (request.getEmail() != null && !request.getEmail().equals(existingUser.getEmail()) &&
            userRepositoryPort.existsByEmail(request.getEmail())) {
            throw new ValidationException("Email already exists: " + request.getEmail());
        }

        userMapper.updateUserFromDto(request, existingUser);

        // Update password if provided
        if (request.getPassword() != null && !request.getPassword().isBlank()) {
            existingUser.setPassword(passwordEncoder.encode(request.getPassword()));
        }

        // Resolve and update relationships if IDs are provided
        if (request.getOrganizationId() != null) {
            existingUser.setOrganization(findOrganizationById(request.getOrganizationId()));
        }
        if (request.getBranchId() != null) {
            existingUser.setBranch(findBranchById(request.getBranchId()));
        }
        if (request.getRoleId() != null) {
            existingUser.setRole(findRoleById(request.getRoleId()));
        }

        String currentUser = securityAuditHelper.getCurrentUsername();
        existingUser.setUpdatedAt(OffsetDateTime.now());
        existingUser.setUpdatedBy(currentUser);

        UserEntity updatedUserEntity = userRepositoryPort.save(userMapper.toUserEntity(existingUser));

        // Audit log
        logAuditChange(currentUser, "USER_UPDATED", updatedUserEntity.getId(), originalSnapshot, updatedUserEntity);

        return userMapper.toUserResponse(userMapper.toUser(updatedUserEntity));
    }

    @Transactional
    public void deleteUser(UUID id) {
        log.info("Deleting user with ID: {}", id);
        UserEntity existingUserEntity = userRepositoryPort.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("User not found with ID: " + id));

        UserEntity originalSnapshot = cloneUserEntity(existingUserEntity);
        String currentUser = securityAuditHelper.getCurrentUsername();

        // Audit log before deletion
        logAuditChange(currentUser, "USER_DELETED", id, originalSnapshot, null);

        userRepositoryPort.deleteById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public List<UserAuditResponse> getUserAuditLogs(UUID id) {
        log.debug("Fetching audit logs for user: {}", id);
        if (userRepositoryPort.findById(id).isEmpty()) {
            throw new EntityNotFoundException("User not found with ID: " + id);
        }

        List<AuditLogEntity> logs = auditLogRepositoryPort.findByEntityTypeAndEntityId("USER", id);

        return logs.stream()
                .map(logEntry -> {
                    String username = "SYSTEM";
                    if (logEntry.getUserId() != null) {
                        username = userRepositoryPort.findById(logEntry.getUserId())
                                .map(u -> u.getUsername())
                                .orElse("UNKNOWN");
                    }
                    List<UserAuditResponse.AuditDetailResponse> detailResponses = logEntry.getDetails().stream()
                            .map(d -> UserAuditResponse.AuditDetailResponse.builder()
                                    .fieldName(d.getFieldName())
                                    .oldValue(d.getOldValue())
                                    .newValue(d.getNewValue())
                                    .build())
                            .collect(Collectors.toList());

                    return UserAuditResponse.builder()
                            .logId(logEntry.getLogId())
                            .action(logEntry.getAction())
                            .username(username)
                            .createdAt(logEntry.getCreatedAt())
                            .details(detailResponses)
                            .build();
                })
                .collect(Collectors.toList());
    }

    // ── Audit & Private Helpers ───────────────────────────────────────────────

    private UserEntity cloneUserEntity(UserEntity source) {
        if (source == null) return null;
        return source.toBuilder().build();
    }

    private void logAuditChange(String username, String action, UUID entityId, UserEntity before, UserEntity after) {
        try {
            UserEntity actor = userRepositoryPort.findByUsername(username).orElse(null);
            if (actor != null) {
                Map<String, Object> beforeState = buildAuditState(before);
                Map<String, Object> afterState = buildAuditState(after);
                auditService.log(actor, action, "USER", entityId, beforeState, afterState);
            }
        } catch (Exception e) {
            log.error("Failed to persist audit log for user operation", e);
        }
    }

    private Map<String, Object> buildAuditState(UserEntity entity) {
        if (entity == null) return null;
        Map<String, Object> state = new HashMap<>();
        state.put("id", entity.getId() != null ? entity.getId().toString() : null);
        state.put("username", entity.getUsername());
        state.put("email", entity.getEmail());
        state.put("firstName", entity.getFirstName());
        state.put("lastName", entity.getLastName());
        state.put("status", entity.getStatus() != null ? entity.getStatus().name() : null);
        state.put("isEnabled", entity.getIsEnabled());
        state.put("changePasswordRequired", entity.getChangePasswordRequired());
        state.put("organizationId", entity.getOrganization() != null ? entity.getOrganization().getId().toString() : null);
        state.put("branchId", entity.getBranch() != null ? entity.getBranch().getId().toString() : null);
        state.put("roleId", entity.getRole() != null ? entity.getRole().getId().toString() : null);
        return state;
    }

    private Organization findOrganizationById(UUID id) {
        return organizationRepositoryPort.findById(id)
                .map(orgEntity -> Organization.builder()
                        .id(orgEntity.getId())
                        .name(orgEntity.getName())
                        .version(orgEntity.getVersion())
                        .build()) // Map to domain model
                .orElseThrow(() -> new EntityNotFoundException("Organization not found with ID: " + id));
    }

    private Branch findBranchById(UUID id) {
        return branchRepositoryPort.findById(id)
                .map(branchEntity -> Branch.builder()
                        .id(branchEntity.getId())
                        .name(branchEntity.getName())
                        .version(branchEntity.getVersion())
                        .build()) // Map to domain model
                .orElseThrow(() -> new EntityNotFoundException("Branch not found with ID: " + id));
    }

    private Role findRoleById(UUID id) {
        return roleRepositoryPort.findById(id)
                .map(roleEntity -> Role.builder()
                        .id(roleEntity.getId())
                        .name(roleEntity.getName())
                        .version(roleEntity.getVersion())
                        .build()) // Map to domain model
                .orElseThrow(() -> new EntityNotFoundException("Role not found with ID: " + id));
    }

    private void resolveUserRelationships(User user) {
        if (user.getOrganization() != null && user.getOrganization().getId() != null) {
            user.setOrganization(findOrganizationById(user.getOrganization().getId()));
        }
        if (user.getBranch() != null && user.getBranch().getId() != null) {
            user.setBranch(findBranchById(user.getBranch().getId()));
        }
        if (user.getRole() != null && user.getRole().getId() != null) {
            user.setRole(findRoleById(user.getRole().getId()));
        }
    }
}