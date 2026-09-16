package com.fourguard.wms.infrastructure.persistence.adapter;

import com.fourguard.wms.domain.ports.out.InventoryItemRepositoryPort;
import com.fourguard.wms.infrastructure.persistence.entity.InventoryItemEntity;
import com.fourguard.wms.infrastructure.persistence.repository.InventoryItemJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class InventoryItemPersistenceAdapter implements InventoryItemRepositoryPort {

    private final InventoryItemJpaRepository repository;

    @Override public Optional<InventoryItemEntity> findById(UUID id)                  { return repository.findById(id); }
    @Override public Optional<InventoryItemEntity> findBySscc(String sscc)            { return repository.findBySscc(sscc); }
    @Override public List<InventoryItemEntity>     findByBranchId(UUID branchId)      { return repository.findByBranchId(branchId); }
    @Override public List<InventoryItemEntity>     findAvailableBySkuFefo(UUID skuId) { return repository.findAvailableBySkuOrderedByFefo(skuId); }
    @Override public InventoryItemEntity           save(InventoryItemEntity item)     { return repository.save(item); }
    @Override public boolean                       existsBySscc(String sscc)         { return repository.existsBySscc(sscc); }
    @Override public int                           updateSapFolioInBranch(UUID branchId, String oldDoc, String newDoc) { return repository.updateSapFolioInBranch(branchId, oldDoc, newDoc); }
}
