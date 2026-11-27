package com.fourguard.wms.application.usecase;

import com.fourguard.wms.domain.enums.UserStatus;
import com.fourguard.wms.domain.ports.out.AuditLogRepositoryPort;
import com.fourguard.wms.infrastructure.persistence.entity.AuditLogDetailEntity;
import com.fourguard.wms.infrastructure.persistence.entity.AuditLogEntity;
import com.fourguard.wms.infrastructure.persistence.entity.UserEntity;
import com.fourguard.wms.infrastructure.persistence.entity.chat.ChatConversationEntity;
import com.fourguard.wms.infrastructure.persistence.entity.chat.ChatParticipantEntity;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.user.SimpUserRegistry;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Shared utility methods for chat presence, user formatting, audit logging, and request metadata.
 */
@Slf4j
final class ChatSupportHelper {

    private ChatSupportHelper() {
    }

    record AuditFieldChange(String fieldName, String oldValue, String newValue) {
    }

    static boolean isUserChatFrozen(UserEntity user, Instant now) {
        return user.getChatFrozenUntil() != null && !user.getChatFrozenUntil().isBefore(now);
    }

    static boolean isUserGovernanceFrozen(UserEntity user, Instant now) {
        return isUserChatFrozen(user, now) || user.getStatus() == UserStatus.SUSPENDED;
    }

    static String buildFullName(UserEntity u) {
        if (u == null) return "Usuario";
        String first = u.getFirstName() != null ? u.getFirstName().trim() : "";
        String last = u.getLastName() != null ? u.getLastName().trim() : "";
        String full = (first + " " + last).trim();
        return full.isEmpty() ? (u.getUsername() != null ? u.getUsername() : "Usuario") : full;
    }

    static String resolveConversationDisplayName(
            ChatConversationEntity conv,
            Map<UUID, List<ChatParticipantEntity>> participantsByConv,
            Map<UUID, UserEntity> usersMap
    ) {
        if (conv == null) return "Conversación";
        if (conv.getName() != null && !conv.getName().isBlank()) {
            return conv.getName();
        }
        if ("DM".equalsIgnoreCase(conv.getType())) {
            List<ChatParticipantEntity> parts = participantsByConv.getOrDefault(conv.getId(), List.of());
            String names = parts.stream()
                    .map(p -> usersMap.get(p.getUserId()))
                    .filter(Objects::nonNull)
                    .map(ChatSupportHelper::buildFullName)
                    .distinct()
                    .collect(Collectors.joining(" / "));
            return names.isBlank() ? "Mensaje Directo" : "DM: " + names;
        }
        return "Espacio sin nombre";
    }

    static Set<UUID> resolveOnlineUserIds(AuditLogRepositoryPort auditLogRepositoryPort) {
        try {
            OffsetDateTime since = OffsetDateTime.now(ZoneOffset.UTC).minusHours(24);
            List<AuditLogEntity> loginEvents = auditLogRepositoryPort.findByActionAndCreatedAtAfter("LOGIN", since);
            if (loginEvents.isEmpty()) return Set.of();

            Map<UUID, AuditLogEntity> latestLogins = loginEvents.stream()
                    .filter(e -> e.getUserId() != null && e.getCreatedAt() != null)
                    .collect(Collectors.toMap(
                            AuditLogEntity::getUserId,
                            e -> e,
                            (a, b) -> a.getCreatedAt().isAfter(b.getCreatedAt()) ? a : b
                    ));

            return latestLogins.values().stream()
                    .filter(login -> auditLogRepositoryPort
                            .findLastLogoutForUserAfter(login.getUserId(), login.getCreatedAt())
                            .isEmpty())
                    .map(AuditLogEntity::getUserId)
                    .collect(Collectors.toSet());
        } catch (Exception ex) {
            log.warn("[CHAT-PRESENCE] Error resolviendo usuarios en línea: {}", ex.getMessage());
            return Set.of();
        }
    }

    static Set<String> resolveOnlineWsUsernames(Optional<SimpUserRegistry> simpUserRegistry) {
        return simpUserRegistry
                .map(reg -> reg.getUsers().stream().map(u -> u.getName()).collect(Collectors.toSet()))
                .orElse(Set.of());
    }

    static HttpServletRequest currentRequestOrNull() {
        if (RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes attrs) {
            return attrs.getRequest();
        }
        return null;
    }

    static String extractClientIp(HttpServletRequest request, int maxLength) {
        HttpServletRequest req = request != null ? request : currentRequestOrNull();
        if (req == null) return "unknown";
        String forwarded = req.getHeader("X-Forwarded-For");
        String ip = (forwarded != null && !forwarded.isBlank())
                ? forwarded.split(",")[0].trim()
                : req.getRemoteAddr();
        if (ip == null || ip.isBlank()) return "unknown";
        return truncate(ip, maxLength);
    }

    static String extractUserAgent(HttpServletRequest request) {
        HttpServletRequest req = request != null ? request : currentRequestOrNull();
        if (req == null) return "unknown";
        String ua = req.getHeader("User-Agent");
        return (ua != null && !ua.isBlank()) ? ua : "unknown";
    }

    static String truncate(String value, int maxLength) {
        if (value == null) return null;
        return value.length() <= maxLength ? value : value.substring(0, maxLength);
    }

    static void recordAudit(
            AuditLogRepositoryPort port,
            UserEntity actor,
            String action,
            String entityType,
            UUID entityId,
            List<AuditFieldChange> changes,
            HttpServletRequest request
    ) {
        if (port == null || actor == null || actor.getOrganization() == null || entityId == null) {
            return;
        }
        AuditLogEntity entry = AuditLogEntity.builder()
                .organizationId(actor.getOrganization().getId())
                .branchId(actor.getBranch() != null ? actor.getBranch().getId() : null)
                .userId(actor.getId())
                .action(action)
                .entityType(entityType)
                .entityId(entityId)
                .ipAddress(extractClientIp(request, 45))
                .userAgent(extractUserAgent(request))
                .build();

        if (changes != null) {
            for (AuditFieldChange change : changes) {
                entry.getDetails().add(AuditLogDetailEntity.builder()
                        .log(entry)
                        .fieldName(change.fieldName())
                        .oldValue(change.oldValue())
                        .newValue(change.newValue())
                        .build());
            }
        }
        port.log(entry);
    }
}
