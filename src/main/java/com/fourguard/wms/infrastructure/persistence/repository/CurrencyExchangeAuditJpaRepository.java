package com.fourguard.wms.infrastructure.persistence.repository;

import com.fourguard.wms.domain.enums.CurrencyAuditAction;
import com.fourguard.wms.domain.enums.CurrencyAuditEntityType;
import com.fourguard.wms.infrastructure.persistence.entity.CurrencyExchangeAuditEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface CurrencyExchangeAuditJpaRepository extends JpaRepository<CurrencyExchangeAuditEntity, UUID> {

    @Query("SELECT a FROM CurrencyExchangeAuditEntity a WHERE a.organizationId = :organizationId " +
           "AND (:entityType IS NULL OR a.entityType = :entityType) " +
           "AND (:entityId IS NULL OR a.entityId = :entityId) " +
           "AND (:action IS NULL OR a.action = :action) " +
           "AND (:startDate IS NULL OR a.performedAt >= :startDate) " +
           "AND (:endDate IS NULL OR a.performedAt <= :endDate) " +
           "ORDER BY a.performedAt DESC")
    List<CurrencyExchangeAuditEntity> findAuditLogsWithFilters(@Param("organizationId") UUID organizationId,
                                                              @Param("entityType") CurrencyAuditEntityType entityType,
                                                              @Param("entityId") UUID entityId,
                                                              @Param("action") CurrencyAuditAction action,
                                                              @Param("startDate") OffsetDateTime startDate,
                                                              @Param("endDate") OffsetDateTime endDate);
}
