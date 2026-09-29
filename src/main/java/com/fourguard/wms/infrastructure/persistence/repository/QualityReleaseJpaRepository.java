package com.fourguard.wms.infrastructure.persistence.repository;

import com.fourguard.wms.domain.enums.ReleaseDestination;
import com.fourguard.wms.infrastructure.persistence.entity.QualityReleaseEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface QualityReleaseJpaRepository extends JpaRepository<QualityReleaseEntity, UUID> {

    Optional<QualityReleaseEntity> findByFolio(String folio);

    Optional<QualityReleaseEntity> findByIncidenceId(UUID incidenceId);

    List<QualityReleaseEntity> findByBranchIdOrderByCreatedAtDesc(UUID branchId);

    List<QualityReleaseEntity> findByBranchIdAndDestinationOrderByCreatedAtDesc(UUID branchId, ReleaseDestination destination);

    @Query("SELECT COUNT(r) FROM QualityReleaseEntity r WHERE r.organization.id = :orgId")
    long countByOrganizationId(@Param("orgId") UUID orgId);
}
