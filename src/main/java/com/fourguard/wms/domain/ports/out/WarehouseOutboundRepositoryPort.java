package com.fourguard.wms.domain.ports.out;

import com.fourguard.wms.infrastructure.persistence.entity.WarehouseOutboundEntity;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Secondary (Driven) Port — Repository contract for Warehouse Outbound persistence.
 * Implemented by {@code WarehouseOutboundPersistenceAdapter}.
 */
public interface WarehouseOutboundRepositoryPort {

    Optional<WarehouseOutboundEntity> findById(UUID id);

    List<WarehouseOutboundEntity> findByOrganizationId(UUID organizationId);

    List<WarehouseOutboundEntity> findByOrganizationIdAndStatus(UUID organizationId, String status);

    WarehouseOutboundEntity save(WarehouseOutboundEntity entity);

    long nextFolioSequenceValue();
}
