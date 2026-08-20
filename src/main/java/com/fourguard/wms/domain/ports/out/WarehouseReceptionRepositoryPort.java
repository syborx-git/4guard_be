package com.fourguard.wms.domain.ports.out;

import com.fourguard.wms.infrastructure.persistence.entity.WarehouseReceptionEntity;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Secondary (Driven) Port — Repository contract for Warehouse Reception persistence.
 * Implemented by {@code WarehouseReceptionPersistenceAdapter}.
 */
public interface WarehouseReceptionRepositoryPort {

    Optional<WarehouseReceptionEntity> findById(UUID id);

    List<WarehouseReceptionEntity> findByOrganizationId(UUID organizationId);

    List<WarehouseReceptionEntity> findByOrganizationIdAndBranchId(UUID organizationId, UUID branchId);

    List<WarehouseReceptionEntity> findByOrganizationIdAndStatus(UUID organizationId, String status);

    Optional<WarehouseReceptionEntity> findByFolio(String folio);

    boolean existsByOrganizationIdAndDocNumber(UUID organizationId, String docNumber);

    WarehouseReceptionEntity save(WarehouseReceptionEntity entity);

    /** Gets the next folio number from the PostgreSQL sequence. */
    long nextFolioSequenceValue();
}
