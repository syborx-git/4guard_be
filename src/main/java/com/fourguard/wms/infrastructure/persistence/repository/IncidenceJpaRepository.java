package com.fourguard.wms.infrastructure.persistence.repository;

import com.fourguard.wms.infrastructure.persistence.entity.IncidenceEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface IncidenceJpaRepository extends JpaRepository<IncidenceEntity, UUID> {
    List<IncidenceEntity> findByItemId(UUID itemId);
    Optional<IncidenceEntity> findByFolio(Integer folio);
    List<IncidenceEntity> findByItemBranchIdOrderByCreatedAtDesc(UUID branchId);
    List<IncidenceEntity> findByItemBranchIdAndStatusOrderByCreatedAtDesc(UUID branchId, com.fourguard.wms.domain.enums.IncidenceStatus status);
    List<IncidenceEntity> findByItemBranchIdAndStageOrderByCreatedAtDesc(UUID branchId, com.fourguard.wms.domain.enums.DetectionStage stage);
}
