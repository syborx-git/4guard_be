package com.fourguard.wms.infrastructure.persistence.repository;

import com.fourguard.wms.domain.enums.ShiftSessionStatus;
import com.fourguard.wms.infrastructure.persistence.entity.ShiftSessionEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface ShiftSessionJpaRepository extends JpaRepository<ShiftSessionEntity, UUID> {

    Optional<ShiftSessionEntity> findFirstByBranch_IdAndStatusOrderByStartedAtDesc(
            UUID branchId, ShiftSessionStatus status);

    Optional<ShiftSessionEntity> findFirstByBranch_IdOrderByStartedAtDesc(UUID branchId);
}
