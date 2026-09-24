package com.fourguard.wms.domain.ports.out;

import com.fourguard.wms.infrastructure.persistence.entity.UaMappingEntity;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface UaMappingRepositoryPort {
    UaMappingEntity save(UaMappingEntity entity);
    List<UaMappingEntity> saveAll(Iterable<UaMappingEntity> entities);
    Optional<UaMappingEntity> findById(UUID id);
    List<UaMappingEntity> findByReceptionId(UUID receptionId);
    List<UaMappingEntity> findByPalletId(UUID palletId);
    Optional<UaMappingEntity> findByInternalUaCode(String internalUaCode);
    Optional<UaMappingEntity> findBySupplierUaCode(String supplierUaCode);
}
