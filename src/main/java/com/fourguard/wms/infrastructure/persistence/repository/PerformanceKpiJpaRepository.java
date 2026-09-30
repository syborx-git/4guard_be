package com.fourguard.wms.infrastructure.persistence.repository;

import com.fourguard.wms.infrastructure.persistence.entity.PerformanceKpiEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PerformanceKpiJpaRepository extends JpaRepository<PerformanceKpiEntity, UUID>, JpaSpecificationExecutor<PerformanceKpiEntity> {

    List<PerformanceKpiEntity> findByIsEnabledTrue();

    List<PerformanceKpiEntity> findByOrganizationIdAndIsEnabledTrue(UUID organizationId);

    List<PerformanceKpiEntity> findByModuleAndIsEnabledTrue(String module);

    Optional<PerformanceKpiEntity> findByIdAndIsEnabledTrue(UUID id);
}
