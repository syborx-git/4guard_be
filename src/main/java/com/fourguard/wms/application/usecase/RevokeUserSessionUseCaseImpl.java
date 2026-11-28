package com.fourguard.wms.application.usecase;

import com.fourguard.wms.domain.exception.EntityNotFoundException;
import com.fourguard.wms.domain.ports.in.RevokeUserSessionUseCase;
import com.fourguard.wms.domain.ports.out.TokenBlacklistPort;
import com.fourguard.wms.domain.ports.out.UserRepositoryPort;
import com.fourguard.wms.infrastructure.persistence.entity.UserEntity;
import com.fourguard.wms.shared.audit.AuditService;
import com.fourguard.wms.shared.audit.SecurityAuditHelper;
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
    private final SecurityAuditHelper securityAuditHelper;

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

        String requestingRoleName = requestingUser.getRole() != null ? requestingUser.getRole().getName() : null;
        boolean hasAuditWrite = requestingUser.getRole() != null
                && requestingUser.getRole().getPermissions() != null
                && requestingUser.getRole().getPermissions().stream()
                        .anyMatch(p -> "AUDIT_WRITE".equals(p.getName()));
        boolean isAdmin = securityAuditHelper.isDirectorGeneralRole(requestingRoleName);
        boolean isOperationsManager = !isAdmin && securityAuditHelper.isOperationsManagerRole(requestingRoleName);

        if (!hasAuditWrite && !isOperationsManager && !isAdmin) {
            throw new AccessDeniedException("No tiene permisos para revocar sesiones de usuario.");
        }

        // 2. Resolve target user
        UserEntity targetUser = userRepositoryPort.findById(targetUserId)
                .orElseThrow(() -> new EntityNotFoundException("Usuario objetivo no encontrado con ID: " + targetUserId));

        // 2b. Operations Manager cannot revoke sessions of Director General users
        if (isOperationsManager
                && targetUser.getRole() != null
                && securityAuditHelper.isDirectorGeneralRole(targetUser.getRole().getName())) {
            throw new EntityNotFoundException("Usuario no encontrado.");
        }

        // 3. Organization scope check for OPERATIONS_MANAGER
        if (!hasAuditWrite && isOperationsManager) {
            if (targetUser.getOrganization() == null
                    || requestingUser.getOrganization() == null
                    || !targetUser.getOrganization().getId().equals(requestingUser.getOrganization().getId())) {
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
