package com.fourguard.wms.infrastructure.persistence.repository;

import com.fourguard.wms.infrastructure.persistence.entity.AuditLogEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface AuditLogJpaRepository extends JpaRepository<AuditLogEntity, UUID>, JpaSpecificationExecutor<AuditLogEntity> {

    /** Uses idx_audit_created index for efficient pagination. */
    Page<AuditLogEntity> findByOrganizationIdOrderByCreatedAtDesc(UUID organizationId, Pageable pageable);

    List<AuditLogEntity> findByEntityTypeAndEntityId(String entityType, UUID entityId);

    List<AuditLogEntity> findByUserId(UUID userId);

    List<AuditLogEntity> findByActionAndCreatedAtAfter(String action, OffsetDateTime since);

    @Query("SELECT a FROM AuditLogEntity a WHERE a.userId = :userId AND a.action IN ('LOGOUT', 'REVOKE_SESSION') AND a.createdAt > :timestamp ORDER BY a.createdAt DESC")
    Optional<AuditLogEntity> findLastLogoutForUserAfter(@Param("userId") UUID userId, @Param("timestamp") OffsetDateTime timestamp);
}



