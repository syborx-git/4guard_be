package com.fourguard.wms.domain.ports.out;

import com.fourguard.wms.application.dto.request.ShiftFilterRequest;
import com.fourguard.wms.infrastructure.persistence.entity.ShiftEntity;

import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public interface ShiftRepositoryPort {

    ShiftEntity save(ShiftEntity entity);

    Optional<ShiftEntity> findById(UUID id);

    Optional<ShiftEntity> findByCodeAndBranchId(String code, UUID branchId);

    List<ShiftEntity> findAll(ShiftFilterRequest filter);

    boolean existsByCodeAndBranchId(String code, UUID branchId);

    boolean existsByCodeAndBranchIdAndIdNot(String code, UUID branchId, UUID id);

    List<ShiftEntity> findOverlappingShifts(UUID branchId, LocalTime startTime, LocalTime endTime, Set<String> days, boolean isOvernight, UUID excludeId);

    void softDelete(UUID id);
}
