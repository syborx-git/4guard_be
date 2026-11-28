package com.fourguard.wms.application.usecase;

import com.fourguard.wms.application.dto.response.audit.UserActivityAuditResponse;
import com.fourguard.wms.domain.exception.EntityNotFoundException;
import com.fourguard.wms.domain.ports.in.GetUserActivityUseCase;
import com.fourguard.wms.domain.ports.out.AuditLogRepositoryPort;
import com.fourguard.wms.domain.ports.out.UserRepositoryPort;
import com.fourguard.wms.infrastructure.persistence.entity.AuditLogEntity;
import com.fourguard.wms.infrastructure.persistence.entity.UserEntity;
import com.fourguard.wms.shared.audit.SecurityAuditHelper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.Principal;
import java.time.OffsetDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Implementation of the {@link GetUserActivityUseCase}.
 * Provides global activity audit filtering with RBAC checks and username enrichment.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class GetUserActivityUseCaseImpl implements GetUserActivityUseCase {

    private final AuditLogRepositoryPort auditLogRepositoryPort;
    private final UserRepositoryPort userRepositoryPort;
    private final SecurityAuditHelper securityAuditHelper;

    @Override
    @Transactional(readOnly = true)
    public List<UserActivityAuditResponse> getUserActivityLogs(
            UUID userId,
            String action,
            OffsetDateTime fromDate,
            OffsetDateTime toDate,
            Principal principal) {

        if (principal == null) {
            throw new AccessDeniedException("No autenticado");
        }

        log.info("[AUDIT] Retrieving user activity logs requested by: {}", principal.getName());

        // 1. Resolve requesting user and enforce RBAC rules
        UserEntity requestingUser = userRepositoryPort.findByUsername(principal.getName())
                .orElseThrow(() -> new EntityNotFoundException("Usuario solicitante no encontrado: " + principal.getName()));

        String requestingRoleName = requestingUser.getRole() != null ? requestingUser.getRole().getName() : null;
        boolean hasAuditRead = requestingUser.getRole() != null
                && requestingUser.getRole().getPermissions() != null
                && requestingUser.getRole().getPermissions().stream()
                        .anyMatch(p -> "AUDIT_READ".equals(p.getName()));

        boolean isDirectorGeneral = securityAuditHelper.isDirectorGeneralRole(requestingRoleName);
        boolean isOperationsManager = !isDirectorGeneral && securityAuditHelper.isOperationsManagerRole(requestingRoleName);

        if (!hasAuditRead && !isOperationsManager && !isDirectorGeneral) {
            throw new AccessDeniedException("No tiene permisos para consultar el historial de auditoría de actividad.");
        }

        // 1b. If Operations Manager filters by a Director General userId, return empty list
        if (isOperationsManager && userId != null) {
            boolean targetIsDirectorGeneral = userRepositoryPort.findById(userId)
                    .map(u -> u.getRole() != null && securityAuditHelper.isDirectorGeneralRole(u.getRole().getName()))
                    .orElse(false);
            if (targetIsDirectorGeneral) {
                return Collections.emptyList();
            }
        }

        // 2. Fetch logs from repository
        String searchAction = (action != null && !action.isBlank()) ? action.trim().toLowerCase() : null;
        List<AuditLogEntity> logs = auditLogRepositoryPort.findUserActivity(userId, searchAction, fromDate, toDate);

        if (logs.isEmpty()) {
            return Collections.emptyList();
        }

        // 3. Batch fetch user information for enrichment
        List<UUID> userIds = logs.stream()
                .map(log -> log.getUserId())
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());

        Map<UUID, UserEntity> userMap = userRepositoryPort.findAllById(userIds).stream()
                .collect(Collectors.toMap(UserEntity::getId, user -> user));

        // 4. Map to response DTOs (hiding Director General activity from Operations Manager)
        return logs.stream()
                .filter(logEntry -> {
                    if (!isOperationsManager || logEntry.getUserId() == null) {
                        return true;
                    }
                    UserEntity logUser = userMap.get(logEntry.getUserId());
                    return logUser == null
                            || logUser.getRole() == null
                            || !securityAuditHelper.isDirectorGeneralRole(logUser.getRole().getName());
                })
                .map(logEntry -> {
                    String username = "SYSTEM";
                    if (logEntry.getUserId() != null) {
                        UserEntity logUser = userMap.get(logEntry.getUserId());
                        username = logUser != null ? logUser.getUsername() : "UNKNOWN";
                    }

                    List<UserActivityAuditResponse.AuditDetailResponse> detailResponses = logEntry.getDetails() != null
                            ? logEntry.getDetails().stream()
                            .map(d -> UserActivityAuditResponse.AuditDetailResponse.builder()
                                    .fieldName(d.getFieldName())
                                    .oldValue(d.getOldValue())
                                    .newValue(d.getNewValue())
                                    .build())
                            .collect(Collectors.toList())
                            : Collections.emptyList();

                    return UserActivityAuditResponse.builder()
                            .logId(logEntry.getLogId())
                            .userId(logEntry.getUserId())
                            .username(username)
                            .action(logEntry.getAction())
                            .entityType(logEntry.getEntityType())
                            .entityId(logEntry.getEntityId())
                            .ipAddress(logEntry.getIpAddress())
                            .userAgent(logEntry.getUserAgent())
                            .createdAt(logEntry.getCreatedAt())
                            .details(detailResponses)
                            .build();
                })
                .collect(Collectors.toList());
    }
}
