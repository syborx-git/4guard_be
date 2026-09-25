package com.fourguard.wms.shared.audit;

import com.fourguard.wms.domain.ports.out.AuditLogRepositoryPort;
import com.fourguard.wms.infrastructure.persistence.entity.AuditLogDetailEntity;
import com.fourguard.wms.infrastructure.persistence.entity.AuditLogEntity;
import com.fourguard.wms.infrastructure.persistence.entity.UserEntity;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.*;

@Service
@RequiredArgsConstructor
public class AuditService {

    private final AuditLogRepositoryPort auditLogRepositoryPort;

    /**
     * Persists a relational audit log entry without JSONB.
     * Automatically extracts IP Address and User-Agent from the request context
     * and calculates field-by-field differences.
     */
    public void log(UserEntity actor, String action, String entityType, UUID entityId, 
                    Map<String, Object> beforeState, Map<String, Object> afterState) {
        if (actor == null) return;
        
        String ipAddress = "unknown";
        String userAgent = "unknown";

        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attributes != null) {
            HttpServletRequest request = attributes.getRequest();
            ipAddress = request.getHeader("X-Forwarded-For");
            if (ipAddress == null || ipAddress.isBlank()) {
                ipAddress = request.getRemoteAddr();
            }
            userAgent = request.getHeader("User-Agent");
        }

        UUID orgId = null;
        try {
            if (actor.getOrganization() != null) {
                orgId = actor.getOrganization().getId();
            }
        } catch (Exception ignored) {}
        if (orgId == null) {
            orgId = UUID.fromString("a53f0907-9fa5-4bdf-87db-2eb5e7683935");
        }

        UUID branchId = null;
        try {
            if (actor.getBranch() != null) {
                branchId = actor.getBranch().getId();
            }
        } catch (Exception ignored) {}

        AuditLogEntity logEntry = AuditLogEntity.builder()
                .organizationId(orgId)
                .branchId(branchId)
                .userId(actor.getId())
                .action(action)
                .entityType(entityType)
                .entityId(entityId)
                .ipAddress(ipAddress)
                .userAgent(userAgent)
                .build();

        List<AuditLogDetailEntity> details = buildDetails(logEntry, beforeState, afterState);
        logEntry.setDetails(details);

        auditLogRepositoryPort.log(logEntry);
    }

    private List<AuditLogDetailEntity> buildDetails(AuditLogEntity logEntry, Map<String, Object> before, Map<String, Object> after) {
        List<AuditLogDetailEntity> details = new ArrayList<>();
        Set<String> allKeys = new HashSet<>();
        if (before != null) allKeys.addAll(before.keySet());
        if (after != null) allKeys.addAll(after.keySet());

        for (String key : allKeys) {
            Object oldVal = before != null ? before.get(key) : null;
            Object newVal = after != null ? after.get(key) : null;

            String oldStr = oldVal != null ? String.valueOf(oldVal).trim() : null;
            String newStr = newVal != null ? String.valueOf(newVal).trim() : null;

            if (isEqual(oldStr, newStr)) {
                continue;
            }

            details.add(AuditLogDetailEntity.builder()
                    .log(logEntry)
                    .fieldName(key)
                    .oldValue(oldStr)
                    .newValue(newStr)
                    .build());
        }
        return details;
    }

    private boolean isEqual(String oldStr, String newStr) {
        if (Objects.equals(oldStr, newStr)) return true;
        if ((oldStr == null || oldStr.isBlank() || "null".equalsIgnoreCase(oldStr)) &&
            (newStr == null || newStr.isBlank() || "null".equalsIgnoreCase(newStr))) {
            return true;
        }

        // Numeric equivalence comparison (e.g., "45.00" == "45.0" == "45")
        if (oldStr != null && newStr != null) {
            try {
                double d1 = Double.parseDouble(oldStr);
                double d2 = Double.parseDouble(newStr);
                if (Math.abs(d1 - d2) < 0.0001) {
                    return true;
                }
            } catch (NumberFormatException ignored) {}
        }

        return false;
    }
}
