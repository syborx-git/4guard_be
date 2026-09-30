package com.fourguard.wms.domain.ports.out;

import com.fourguard.wms.infrastructure.persistence.entity.WarehouseReceptionLotEntity;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Secondary (Driven) Port — Repository contract for Reception Lot persistence.
 * Implemented by {@code WarehouseReceptionLotPersistenceAdapter}.
 */
public interface WarehouseReceptionLotRepositoryPort {

    List<WarehouseReceptionLotEntity> findByReceptionId(UUID receptionId);

    Optional<WarehouseReceptionLotEntity> findById(UUID id);

    Optional<WarehouseReceptionLotEntity> findByReceptionIdAndId(UUID receptionId, UUID lotId);

    Optional<WarehouseReceptionLotEntity> findByReceptionIdAndLotNumber(UUID receptionId, String lotNumber);

    boolean existsByReceptionIdAndLotNumber(UUID receptionId, String lotNumber);

    WarehouseReceptionLotEntity save(WarehouseReceptionLotEntity entity);

    void deleteById(UUID id);

    void deleteByReceptionIdAndId(UUID receptionId, UUID lotId);
}
