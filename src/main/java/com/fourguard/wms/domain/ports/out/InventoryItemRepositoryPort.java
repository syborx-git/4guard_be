package com.fourguard.wms.domain.ports.out;

import com.fourguard.wms.infrastructure.persistence.entity.InventoryItemEntity;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Port OUT — InventoryItem repository contract. */
public interface InventoryItemRepositoryPort {
    Optional<InventoryItemEntity> findById(UUID id);
    Optional<InventoryItemEntity> findBySscc(String sscc);
    List<InventoryItemEntity>     findByBranchId(UUID branchId);
    List<InventoryItemEntity>     findAvailableBySkuFefo(UUID skuId);
    InventoryItemEntity           save(InventoryItemEntity item);
    boolean                       existsBySscc(String sscc);
    int                           updateSapFolioInBranch(UUID branchId, String oldDoc, String newDoc);
}
