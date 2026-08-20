package com.fourguard.wms.application.usecase;

import com.fourguard.wms.domain.exception.EntityNotFoundException;
import com.fourguard.wms.domain.ports.in.RevokeUserSessionUseCase;
import com.fourguard.wms.domain.ports.out.TokenBlacklistPort;
import com.fourguard.wms.domain.ports.out.UserRepositoryPort;
import com.fourguard.wms.infrastructure.persistence.entity.UserEntity;
import com.fourguard.wms.shared.audit.AuditService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.Principal;
import java.util.Map;
import java.util.UUID;

/**
 * Implementation of {@link RevokeUserSessionUseCase}.
 * Revokes active sessions for a target user and logs the audit action.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RevokeUserSessionUseCaseImpl implements RevokeUserSessionUseCase {

    private final UserRepositoryPort userRepositoryPort;
    private final TokenBlacklistPort tokenBlacklistPort;
    private final AuditService auditService;

    @Override
    @Transactional
    public void revokeUserSession(UUID targetUserId, Principal principal) {
        if (principal == null) {
            throw new AccessDeniedException("No autenticado");
        }

        log.info("[AUDIT] Session revocation requested for targetUserId: {} by admin: {}", targetUserId, principal.getName());

        // 1. Resolve requesting admin user and enforce RBAC rules
        UserEntity requestingUser = userRepositoryPort.findByUsername(principal.getName())
                .orElseThrow(() -> new EntityNotFoundException("Usuario solicitante no encontrado: " + principal.getName()));

        boolean hasAuditWrite = requestingUser.getRole().getPermissions().stream()
                .anyMatch(p -> "AUDIT_WRITE".equals(p.getName()));
        boolean isOperationsManager = "OPERATIONS_MANAGER".equals(requestingUser.getRole().getName());
        boolean isAdmin = "ADMIN".equals(requestingUser.getRole().getName()) || "SUPER_ADMIN".equals(requestingUser.getRole().getName());

        if (!hasAuditWrite && !isOperationsManager && !isAdmin) {
            throw new AccessDeniedException("No tiene permisos para revocar sesiones de usuario.");
        }

        // 2. Resolve target user
        UserEntity targetUser = userRepositoryPort.findById(targetUserId)
                .orElseThrow(() -> new EntityNotFoundException("Usuario objetivo no encontrado con ID: " + targetUserId));

        // 3. Organization scope check for OPERATIONS_MANAGER
        if (!hasAuditWrite && isOperationsManager) {
            if (!targetUser.getOrganization().getId().equals(requestingUser.getOrganization().getId())) {
                throw new AccessDeniedException("No tiene permisos para revocar sesiones de usuarios de otra organización.");
            }
        }

        // 4. Register revocation in blacklist cache
        tokenBlacklistPort.revokeUserSessions(targetUserId);

        // 5. Persist REVOKE_SESSION event to database audit log
        auditService.log(
                targetUser,
                "REVOKE_SESSION",
                "USER",
                targetUserId,
                null,
                Map.of(
                        "revokedBy", requestingUser.getUsername(),
                        "targetUsername", targetUser.getUsername(),
                        "description", "Sesión revocada por el administrador " + requestingUser.getUsername()
                )
        );

        log.info("[AUDIT] Session for user '{}' ({}) successfully revoked by '{}'",
                targetUser.getUsername(), targetUserId, requestingUser.getUsername());
    }
}
