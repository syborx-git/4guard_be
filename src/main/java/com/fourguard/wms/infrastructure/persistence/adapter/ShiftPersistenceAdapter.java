package com.fourguard.wms.infrastructure.persistence.adapter;

import com.fourguard.wms.application.dto.request.ShiftFilterRequest;
import com.fourguard.wms.domain.enums.ShiftStatus;
import com.fourguard.wms.domain.ports.out.ShiftRepositoryPort;
import com.fourguard.wms.infrastructure.persistence.entity.ShiftEntity;
import com.fourguard.wms.infrastructure.persistence.repository.ShiftJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class ShiftPersistenceAdapter implements ShiftRepositoryPort {

    private final ShiftJpaRepository repository;

    @Override
    public ShiftEntity save(ShiftEntity entity) {
        return repository.save(entity);
    }

    @Override
    public Optional<ShiftEntity> findById(UUID id) {
        return repository.findByIdAndIsDeletedFalse(id);
    }

    @Override
    public Optional<ShiftEntity> findByCodeAndBranchId(String code, UUID branchId) {
        return repository.findByCodeAndBranchIdAndIsDeletedFalse(code, branchId);
    }

    @Override
    public List<ShiftEntity> findAll(ShiftFilterRequest filter) {
        return repository.findWithFilters(
                filter.getBranchId(),
                filter.getWarehouseSectionId(),
                filter.getStatus(),
                filter.getScopeType(),
                filter.getDayOfWeek(),
                filter.getSearch()
        );
    }

    @Override
    public boolean existsByCodeAndBranchId(String code, UUID branchId) {
        return repository.existsByCodeAndBranchIdAndIsDeletedFalse(code, branchId);
    }

    @Override
    public boolean existsByCodeAndBranchIdAndIdNot(String code, UUID branchId, UUID id) {
        return repository.existsByCodeAndBranchIdAndIdNotAndIsDeletedFalse(code, branchId, id);
    }

    @Override
    public List<ShiftEntity> findOverlappingShifts(UUID branchId, LocalTime startTime, LocalTime endTime, Set<String> days, boolean isOvernight, UUID excludeId) {
        return repository.findOverlappingShifts(branchId, startTime, endTime, days, isOvernight, excludeId);
    }

    @Override
    public void softDelete(UUID id) {
        repository.findById(id).ifPresent(shift -> {
            shift.setIsDeleted(true);
            shift.setStatus(ShiftStatus.INACTIVE);
            repository.save(shift);
        });
    }
}
