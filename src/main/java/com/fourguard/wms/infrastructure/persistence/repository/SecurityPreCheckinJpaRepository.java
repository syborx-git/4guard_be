package com.fourguard.wms.infrastructure.persistence.repository;

import com.fourguard.wms.infrastructure.persistence.entity.SecurityPreCheckinEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface SecurityPreCheckinJpaRepository extends JpaRepository<SecurityPreCheckinEntity, UUID> {

    Optional<SecurityPreCheckinEntity> findByToken(String token);

    Optional<SecurityPreCheckinEntity> findByGeneratedFolio(String generatedFolio);

    @Query("SELECT p FROM SecurityPreCheckinEntity p " +
           "WHERE p.organization.id = :orgId " +
           "AND (:branchId IS NULL OR p.branch.id = :branchId) " +
           "AND p.status IN ('PENDING_DRIVER', 'SUBMITTED') " +
           "AND p.expiresAt > :now " +
           "ORDER BY p.createdAt DESC")
    List<SecurityPreCheckinEntity> findActivePasses(
            @Param("orgId") UUID orgId,
            @Param("branchId") UUID branchId,
            @Param("now") OffsetDateTime now
    );

    @Query("SELECT p FROM SecurityPreCheckinEntity p " +
           "WHERE p.organization.id = :orgId " +
           "AND (:branchId IS NULL OR p.branch.id = :branchId) " +
           "AND p.status = 'COMPLETED' " +
           "ORDER BY p.processedAt DESC")
    List<SecurityPreCheckinEntity> findInYardPasses(
            @Param("orgId") UUID orgId,
            @Param("branchId") UUID branchId
    );

    @Query("SELECT p FROM SecurityPreCheckinEntity p " +
           "WHERE p.organization.id = :orgId " +
           "AND (:branchId IS NULL OR p.branch.id = :branchId) " +
           "AND p.status = 'COMPLETED_EXIT' " +
           "ORDER BY p.exitedAt DESC, p.createdAt DESC")
    List<SecurityPreCheckinEntity> findHistoryPasses(
            @Param("orgId") UUID orgId,
            @Param("branchId") UUID branchId
    );

    @Query("SELECT p FROM SecurityPreCheckinEntity p " +
           "WHERE p.organization.id = :orgId " +
           "AND (:branchId IS NULL OR p.branch.id = :branchId) " +
           "AND p.status = 'COMPLETED_EXIT' " +
           "AND ((:shiftSessionId IS NOT NULL AND p.shiftSessionId = :shiftSessionId) " +
           "     OR (:shiftSessionId IS NULL AND (:since IS NULL OR p.exitedAt >= :since))) " +
           "ORDER BY p.exitedAt DESC, p.createdAt DESC")
    List<SecurityPreCheckinEntity> findCurrentShiftHistoryPasses(
            @Param("orgId") UUID orgId,
            @Param("branchId") UUID branchId,
            @Param("shiftSessionId") UUID shiftSessionId,
            @Param("since") OffsetDateTime since
    );

    @Query("SELECT p FROM SecurityPreCheckinEntity p " +
           "WHERE p.organization.id = :orgId " +
           "AND (:branchId IS NULL OR p.branch.id = :branchId) " +
           "AND p.status = 'COMPLETED_EXIT' " +
           "AND (:from IS NULL OR p.exitedAt >= :from) " +
           "AND (:to IS NULL OR p.exitedAt <= :to) " +
           "ORDER BY p.exitedAt DESC, p.createdAt DESC")
    List<SecurityPreCheckinEntity> findGlobalHistoryPasses(
            @Param("orgId") UUID orgId,
            @Param("branchId") UUID branchId,
            @Param("from") OffsetDateTime from,
            @Param("to") OffsetDateTime to
    );
}
