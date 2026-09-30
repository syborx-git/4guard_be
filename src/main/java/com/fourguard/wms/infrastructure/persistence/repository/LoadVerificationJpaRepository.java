package com.fourguard.wms.infrastructure.persistence.repository;

import com.fourguard.wms.domain.enums.LoadVerificationStatus;
import com.fourguard.wms.infrastructure.persistence.entity.LoadVerificationEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface LoadVerificationJpaRepository extends JpaRepository<LoadVerificationEntity, UUID> {

    Optional<LoadVerificationEntity> findByFolio(String folio);

    Optional<LoadVerificationEntity> findByBranchIdAndRemisionNumber(UUID branchId, String remisionNumber);

    List<LoadVerificationEntity> findByBranchIdOrderByCreatedAtDesc(UUID branchId);

    List<LoadVerificationEntity> findByBranchIdAndStatusOrderByCreatedAtDesc(UUID branchId, LoadVerificationStatus status);

    List<LoadVerificationEntity> findByBranchIdAndVerificationDateOrderByCreatedAtDesc(UUID branchId, LocalDate verificationDate);

    @Query("SELECT COUNT(v) FROM LoadVerificationEntity v WHERE v.organization.id = :orgId")
    long countByOrganizationId(@Param("orgId") UUID orgId);
}
