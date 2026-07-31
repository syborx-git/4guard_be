package com.fourguard.wms.domain.ports.out;

import com.fourguard.wms.infrastructure.persistence.entity.AuditLogEntity;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Port OUT — AuditLog repository contract. Write-only by design (WORM). */
public interface AuditLogRepositoryPort {
    /** Creates an immutable audit log entry. Never updates or deletes. */
    AuditLogEntity log(AuditLogEntity entry);
    List<AuditLogEntity> findByEntityTypeAndEntityId(String entityType, UUID entityId);
    List<AuditLogEntity> findByUserId(UUID userId);
    List<AuditLogEntity> findByActionAndCreatedAtAfter(String action, OffsetDateTime since);
    Optional<AuditLogEntity> findLastLogoutForUserAfter(UUID userId, OffsetDateTime timestamp);
    List<AuditLogEntity> findUserActivity(UUID userId, String action, OffsetDateTime fromDate, OffsetDateTime toDate);
}


