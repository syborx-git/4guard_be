package com.fourguard.wms.infrastructure.persistence.repository;

import com.fourguard.wms.infrastructure.persistence.entity.QualityDeviationEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface QualityDeviationJpaRepository extends
        JpaRepository<QualityDeviationEntity, UUID>,
        JpaSpecificationExecutor<QualityDeviationEntity> {

    Optional<QualityDeviationEntity> findByFolio(String folio);

    List<QualityDeviationEntity> findByBranchIdOrderByDeviationDateDescCreatedAtDesc(UUID branchId);

    List<QualityDeviationEntity> findByBranchIdAndDeviationDateBetweenOrderByDeviationDateDesc(
            UUID branchId, LocalDate startDate, LocalDate endDate);

    @Query("SELECT COUNT(d) FROM QualityDeviationEntity d WHERE d.organization.id = :orgId")
    long countByOrganizationId(@Param("orgId") UUID orgId);
}
