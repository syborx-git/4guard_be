package com.fourguard.wms.application.usecase;

import com.fourguard.wms.domain.exception.EntityNotFoundException;
import com.fourguard.wms.domain.ports.in.ResetUserPasswordUseCase;
import com.fourguard.wms.domain.ports.out.UserRepositoryPort;
import com.fourguard.wms.infrastructure.persistence.entity.UserEntity;
import com.fourguard.wms.shared.audit.SecurityAuditHelper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class ResetUserPasswordUseCaseImpl implements ResetUserPasswordUseCase {

    private final UserRepositoryPort userRepositoryPort;
    private final PasswordEncoder passwordEncoder;
    private final com.fourguard.wms.shared.audit.AuditService auditService;
    private final SecurityAuditHelper securityAuditHelper;

    @Override
    @Transactional
    public String resetToTemporaryPasswordById(UUID userId, String adminUsername) {
        log.info("[AUTH] Admin '{}' requested temporary password reset for user ID: {}", adminUsername, userId);

        UserEntity userEntity = userRepositoryPort.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("No se encontró ningún usuario con el ID: " + userId));

        return executeReset(userEntity, adminUsername);
    }

    @Override
    @Transactional
    public String resetToTemporaryPassword(String usernameOrEmail, String adminUsername) {
        log.info("[AUTH] Admin '{}' requested temporary password reset for user/email: {}", adminUsername, usernameOrEmail);

        // Buscar el usuario por username o email; lanzar error descriptivo si no existe
        UserEntity userEntity = userRepositoryPort.findByUsernameOrEmail(usernameOrEmail)
                .orElseThrow(() -> new EntityNotFoundException(
                        "No se encontró ningún usuario con el nombre de usuario o correo electrónico: '" + usernameOrEmail + "'"));

        return executeReset(userEntity, adminUsername);
    }

    private String executeReset(UserEntity userEntity, String adminUsername) {
        if (securityAuditHelper.isCurrentUserOperationsManager()
                && userEntity.getRole() != null
                && securityAuditHelper.isDirectorGeneralRole(userEntity.getRole().getName())) {
            throw new EntityNotFoundException("Usuario no encontrado.");
        }

        UUID userId = userEntity.getId();
        UserEntity adminEntity = resolveAuditActor(adminUsername, userEntity);

        // Generate temporary password with complexity: 4G-<8 chars hex>-*
        String tempPassword = "4G-" + UUID.randomUUID().toString().substring(0, 8) + "*";

        userEntity.setPassword(passwordEncoder.encode(tempPassword));
        userEntity.setChangePasswordRequired(true);
        userEntity.setUpdatedAt(OffsetDateTime.now());
        userEntity.setUpdatedBy(adminUsername);

        userRepositoryPort.save(userEntity);

        auditService.log(adminEntity, "PASSWORD_RESET_TEMP", "USER", userId, null, java.util.Map.of("targetUsername", userEntity.getUsername(), "initiatedBy", adminUsername));

        log.info("[AUDIT] Temporary password generated successfully for user '{}' (ID: {})", userEntity.getUsername(), userId);
        return tempPassword;
    }

    private UserEntity resolveAuditActor(String adminUsername, UserEntity fallbackUser) {
        if ("SYSTEM".equals(adminUsername)) {
            return fallbackUser;
        }

        return userRepositoryPort.findByUsername(adminUsername)
                .orElseThrow(() -> new EntityNotFoundException("Admin not found with username: " + adminUsername));
    }
}
