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
}
