package com.fourguard.wms.domain.ports.out;

import com.fourguard.wms.infrastructure.persistence.entity.WarehouseReceptionPalletEntity;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Secondary (Driven) Port — Repository contract for Reception Pallet persistence.
 * Implemented by {@code WarehouseReceptionPalletPersistenceAdapter}.
 */
public interface WarehouseReceptionPalletRepositoryPort {

    List<WarehouseReceptionPalletEntity> findByReceptionId(UUID receptionId);

    Optional<WarehouseReceptionPalletEntity> findById(UUID id);

    Optional<WarehouseReceptionPalletEntity> findByReceptionIdAndId(UUID receptionId, UUID palletId);

    Optional<WarehouseReceptionPalletEntity> findByReceptionIdAndPalletCode(UUID receptionId, String palletCode);

    boolean existsByReceptionIdAndPalletCode(UUID receptionId, String palletCode);

    WarehouseReceptionPalletEntity save(WarehouseReceptionPalletEntity entity);

    void deleteById(UUID id);

    int countByReceptionId(UUID receptionId);
}
