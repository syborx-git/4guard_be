package com.fourguard.wms.domain.ports.out;

import com.fourguard.wms.infrastructure.persistence.entity.WarehouseTransferEntity;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Secondary (Driven) Port — Repository contract for Warehouse Transfer persistence.
 * Implemented by {@code WarehouseTransferPersistenceAdapter}.
 */
public interface WarehouseTransferRepositoryPort {

    Optional<WarehouseTransferEntity> findById(UUID id);

    List<WarehouseTransferEntity> findByOrganizationId(UUID organizationId);

    List<WarehouseTransferEntity> findByOrganizationIdAndStatus(UUID organizationId, String status);

    WarehouseTransferEntity save(WarehouseTransferEntity entity);

    long nextFolioSequenceValue();
}
