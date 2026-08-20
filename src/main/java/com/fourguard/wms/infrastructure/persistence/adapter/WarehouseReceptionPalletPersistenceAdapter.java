package com.fourguard.wms.infrastructure.persistence.adapter;

import com.fourguard.wms.domain.ports.out.WarehouseReceptionPalletRepositoryPort;
import com.fourguard.wms.infrastructure.persistence.entity.WarehouseReceptionPalletEntity;
import com.fourguard.wms.infrastructure.persistence.repository.WarehouseReceptionPalletJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class WarehouseReceptionPalletPersistenceAdapter implements WarehouseReceptionPalletRepositoryPort {

    private final WarehouseReceptionPalletJpaRepository repository;

    @Override
    public List<WarehouseReceptionPalletEntity> findByReceptionId(UUID receptionId) {
        return repository.findByReceptionIdOrderByPalletNumberAsc(receptionId);
    }

    @Override
    public Optional<WarehouseReceptionPalletEntity> findById(UUID id) {
        return repository.findById(id);
    }

    @Override
    public Optional<WarehouseReceptionPalletEntity> findByReceptionIdAndId(UUID receptionId, UUID palletId) {
        return repository.findByReceptionIdAndId(receptionId, palletId);
    }

    @Override
    public boolean existsByReceptionIdAndPalletCode(UUID receptionId, String palletCode) {
        return repository.existsByReceptionIdAndPalletCode(receptionId, palletCode);
    }

    @Override
    public WarehouseReceptionPalletEntity save(WarehouseReceptionPalletEntity entity) {
        return repository.save(entity);
    }

    @Override
    public void deleteById(UUID id) {
        repository.deleteById(id);
    }

    @Override
    public int countByReceptionId(UUID receptionId) {
        return repository.countByReceptionId(receptionId);
    }
}
