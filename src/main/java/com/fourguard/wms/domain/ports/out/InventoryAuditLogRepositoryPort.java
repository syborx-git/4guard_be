package com.fourguard.wms.domain.ports.out;

import com.fourguard.wms.infrastructure.persistence.entity.InventoryAuditLogEntity;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface InventoryAuditLogRepositoryPort {
    InventoryAuditLogEntity save(InventoryAuditLogEntity entity);
    List<InventoryAuditLogEntity> saveAll(Iterable<InventoryAuditLogEntity> entities);
    Optional<InventoryAuditLogEntity> findById(UUID id);
    List<InventoryAuditLogEntity> findByOrganizationId(UUID organizationId);
    List<InventoryAuditLogEntity> findByPalletCode(String palletCode);
    List<InventoryAuditLogEntity> findByRemisionFolio(String remisionFolio);
    List<InventoryAuditLogEntity> findByPalletId(UUID palletId);
}
