package com.fourguard.wms.application.usecase;

import com.fourguard.wms.application.dto.response.audit.ActiveSessionResponse;
import com.fourguard.wms.application.mapper.ActiveSessionMapper;
import com.fourguard.wms.domain.exception.EntityNotFoundException;
import com.fourguard.wms.domain.ports.in.GetActiveSessionsUseCase;
import com.fourguard.wms.domain.ports.out.AuditLogRepositoryPort;
import com.fourguard.wms.domain.ports.out.UserRepositoryPort;
import com.fourguard.wms.infrastructure.persistence.entity.AuditLogEntity;
import com.fourguard.wms.infrastructure.persistence.entity.UserEntity;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.Principal;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Implementation of the {@link GetActiveSessionsUseCase}.
 * Manages security constraints, fetches audit logs, filters active sessions, and enriches user information.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class GetActiveSessionsUseCaseImpl implements GetActiveSessionsUseCase {

    private final AuditLogRepositoryPort auditLogRepositoryPort;
    private final UserRepositoryPort userRepositoryPort;
    private final ActiveSessionMapper activeSessionMapper;

    @Override
    @Transactional(readOnly = true)
    public List<ActiveSessionResponse> getActiveSessions(UUID organizationId, UUID branchId, Principal principal) {
        if (principal == null) {
            throw new AccessDeniedException("No autenticado");
        }

        log.info("[AUDIT] Retrieving active sessions requested by: {}", principal.getName());

        // 1. Resolve requesting user and enforce RBAC rules
        UserEntity requestingUser = userRepositoryPort.findByUsername(principal.getName())
                .orElseThrow(() -> new EntityNotFoundException("Usuario solicitante no encontrado: " + principal.getName()));

        boolean hasAuditRead = requestingUser.getRole().getPermissions().stream()
                .anyMatch(p -> "AUDIT_READ".equals(p.getName()));

        boolean isOperationsManager = "OPERATIONS_MANAGER".equals(requestingUser.getRole().getName());

        UUID targetOrganizationId = organizationId;
        UUID targetBranchId = branchId;

        if (!hasAuditRead) {
            if (isOperationsManager) {
                UUID userOrgId = requestingUser.getOrganization().getId();
                if (organizationId != null && !organizationId.equals(userOrgId)) {
                    throw new AccessDeniedException("No tiene permisos para consultar información de otra organización.");
                }
                targetOrganizationId = userOrgId;
            } else {
                throw new AccessDeniedException("No tiene permisos para consultar las sesiones activas.");
            }
        }

        // 2. Fetch all logins from the last 24 hours
        OffsetDateTime since = OffsetDateTime.now(ZoneOffset.UTC).minusHours(24);
        List<AuditLogEntity> loginEvents = auditLogRepositoryPort.findByActionAndCreatedAtAfter("LOGIN", since);

        if (loginEvents.isEmpty()) {
            return Collections.emptyList();
        }

        // 3. Group by userId and pick the latest login event for each user
        Map<UUID, AuditLogEntity> latestLoginsByUser = loginEvents.stream()
                .collect(Collectors.toMap(
                        event -> event.getUserId(),
                        event -> event,
                        (existing, replacement) -> existing.getCreatedAt().isAfter(replacement.getCreatedAt()) ? existing : replacement
                ));

        // 4. Filter out users who have logged out after their latest login event
        List<AuditLogEntity> activeLogins = latestLoginsByUser.values().stream()
                .filter(login -> {
                    Optional<AuditLogEntity> lastLogout = auditLogRepositoryPort.findLastLogoutForUserAfter(
                            login.getUserId(), login.getCreatedAt());
                    return lastLogout.isEmpty();
                })
                .collect(Collectors.toList());

        if (activeLogins.isEmpty()) {
            return Collections.emptyList();
        }

        // 5. Enrich session records with user data in an optimized batch query
        List<UUID> activeUserIds = activeLogins.stream()
                .map(event -> event.getUserId())
                .distinct()
                .collect(Collectors.toList());

        List<UserEntity> users = userRepositoryPort.findAllById(activeUserIds);
        Map<UUID, UserEntity> userMap = users.stream()
                .collect(Collectors.toMap(user -> user.getId(), user -> user));

        // 6. Filter by target organization/branch and map to DTO response
        List<ActiveSessionResponse> responseList = new ArrayList<>();
        for (AuditLogEntity loginEvent : activeLogins) {
            UserEntity user = userMap.get(loginEvent.getUserId());
            if (user == null) {
                continue;
            }

            // Organization filter
            if (targetOrganizationId != null && !user.getOrganization().getId().equals(targetOrganizationId)) {
                continue;
            }

            // Branch filter
            if (targetBranchId != null && (user.getBranch() == null || !user.getBranch().getId().equals(targetBranchId))) {
                continue;
            }

            responseList.add(activeSessionMapper.toActiveSessionResponse(user, loginEvent));
        }

        // 7. Sort by login timestamp descending (newest first)
        responseList.sort((a, b) -> b.getLastLoginAt().compareTo(a.getLastLoginAt()));

        return responseList;
    }
}
