package com.fourguard.wms.domain.ports.out;

import com.fourguard.wms.infrastructure.persistence.entity.ShiftSessionEntity;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ShiftSessionRepositoryPort {

    ShiftSessionEntity save(ShiftSessionEntity session);

    Optional<ShiftSessionEntity> findById(UUID id);

    Optional<ShiftSessionEntity> findOpenByBranchId(UUID branchId);

    Optional<ShiftSessionEntity> findLatestByBranchId(UUID branchId);

    List<ShiftSessionEntity> findByIdIn(Collection<UUID> ids);
}
