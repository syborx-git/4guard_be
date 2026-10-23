package com.fourguard.wms.infrastructure.persistence.adapter;

import com.fourguard.wms.domain.enums.ShiftSessionStatus;
import com.fourguard.wms.domain.ports.out.ShiftSessionRepositoryPort;
import com.fourguard.wms.infrastructure.persistence.entity.ShiftSessionEntity;
import com.fourguard.wms.infrastructure.persistence.repository.ShiftSessionJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class ShiftSessionPersistenceAdapter implements ShiftSessionRepositoryPort {

    private final ShiftSessionJpaRepository repository;

    @Override
    public ShiftSessionEntity save(ShiftSessionEntity session) {
        return repository.save(session);
    }

    @Override
    public Optional<ShiftSessionEntity> findById(UUID id) {
        return repository.findById(id);
    }

    @Override
    public Optional<ShiftSessionEntity> findOpenByBranchId(UUID branchId) {
        return repository.findFirstByBranch_IdAndStatusOrderByStartedAtDesc(branchId, ShiftSessionStatus.OPEN);
    }

    @Override
    public Optional<ShiftSessionEntity> findLatestByBranchId(UUID branchId) {
        return repository.findFirstByBranch_IdOrderByStartedAtDesc(branchId);
    }

    @Override
    public List<ShiftSessionEntity> findByIdIn(Collection<UUID> ids) {
        if (ids == null || ids.isEmpty()) return List.of();
        return repository.findAllById(ids);
    }
}
