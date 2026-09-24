package com.fourguard.wms.infrastructure.persistence.adapter;

import com.fourguard.wms.domain.ports.out.UaMappingRepositoryPort;
import com.fourguard.wms.infrastructure.persistence.entity.UaMappingEntity;
import com.fourguard.wms.infrastructure.persistence.repository.UaMappingJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class UaMappingPersistenceAdapter implements UaMappingRepositoryPort {

    private final UaMappingJpaRepository jpaRepository;

    @Override
    public UaMappingEntity save(UaMappingEntity entity) {
        return jpaRepository.save(entity);
    }

    @Override
    public List<UaMappingEntity> saveAll(Iterable<UaMappingEntity> entities) {
        return jpaRepository.saveAll(entities);
    }

    @Override
    public Optional<UaMappingEntity> findById(UUID id) {
        return jpaRepository.findById(id);
    }

    @Override
    public List<UaMappingEntity> findByReceptionId(UUID receptionId) {
        return jpaRepository.findByReceptionId(receptionId);
    }

    @Override
    public List<UaMappingEntity> findByPalletId(UUID palletId) {
        return jpaRepository.findByPalletId(palletId);
    }

    @Override
    public Optional<UaMappingEntity> findByInternalUaCode(String internalUaCode) {
        return jpaRepository.findByInternalUaCode(internalUaCode);
    }

    @Override
    public Optional<UaMappingEntity> findBySupplierUaCode(String supplierUaCode) {
        return jpaRepository.findBySupplierUaCode(supplierUaCode);
    }
}
