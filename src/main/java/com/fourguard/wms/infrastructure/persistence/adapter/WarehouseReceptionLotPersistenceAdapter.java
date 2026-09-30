package com.fourguard.wms.infrastructure.persistence.adapter;

import com.fourguard.wms.domain.ports.out.WarehouseReceptionLotRepositoryPort;
import com.fourguard.wms.infrastructure.persistence.entity.WarehouseReceptionLotEntity;
import com.fourguard.wms.infrastructure.persistence.repository.WarehouseReceptionLotJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class WarehouseReceptionLotPersistenceAdapter implements WarehouseReceptionLotRepositoryPort {

    private final WarehouseReceptionLotJpaRepository repository;

    @Override
    public List<WarehouseReceptionLotEntity> findByReceptionId(UUID receptionId) {
        return repository.findByReceptionIdOrderByCreatedAtAsc(receptionId);
    }

    @Override
    public Optional<WarehouseReceptionLotEntity> findById(UUID id) {
        return repository.findById(id);
    }

    @Override
    public Optional<WarehouseReceptionLotEntity> findByReceptionIdAndId(UUID receptionId, UUID lotId) {
        return repository.findByReceptionIdAndId(receptionId, lotId);
    }

    @Override
    public Optional<WarehouseReceptionLotEntity> findByReceptionIdAndLotNumber(UUID receptionId, String lotNumber) {
        return repository.findByReceptionIdAndLotNumber(receptionId, lotNumber);
    }

    @Override
    public boolean existsByReceptionIdAndLotNumber(UUID receptionId, String lotNumber) {
        return repository.existsByReceptionIdAndLotNumber(receptionId, lotNumber);
    }

    @Override
    public WarehouseReceptionLotEntity save(WarehouseReceptionLotEntity entity) {
        return repository.save(entity);
    }

    @Override
    public void deleteById(UUID id) {
        repository.deleteById(id);
    }

    @Override
    public void deleteByReceptionIdAndId(UUID receptionId, UUID lotId) {
        repository.deleteByReceptionIdAndId(receptionId, lotId);
    }
}
