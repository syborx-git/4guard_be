package com.fourguard.wms.infrastructure.persistence.adapter;

import com.fourguard.wms.domain.ports.out.InventoryAuditLogRepositoryPort;
import com.fourguard.wms.infrastructure.persistence.entity.InventoryAuditLogEntity;
import com.fourguard.wms.infrastructure.persistence.repository.InventoryAuditLogJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class InventoryAuditLogPersistenceAdapter implements InventoryAuditLogRepositoryPort {

    private final InventoryAuditLogJpaRepository jpaRepository;

    @Override
    public InventoryAuditLogEntity save(InventoryAuditLogEntity entity) {
        return jpaRepository.save(entity);
    }

    @Override
    public List<InventoryAuditLogEntity> saveAll(Iterable<InventoryAuditLogEntity> entities) {
        return jpaRepository.saveAll(entities);
    }

    @Override
    public Optional<InventoryAuditLogEntity> findById(UUID id) {
        return jpaRepository.findById(id);
    }

    @Override
    public List<InventoryAuditLogEntity> findByOrganizationId(UUID organizationId) {
        return jpaRepository.findByOrganizationIdOrderByPerformedAtDesc(organizationId);
    }

    @Override
    public List<InventoryAuditLogEntity> findByPalletCode(String palletCode) {
        return jpaRepository.findByPalletCodeOrderByPerformedAtAsc(palletCode);
    }

    @Override
    public List<InventoryAuditLogEntity> findByRemisionFolio(String remisionFolio) {
        return jpaRepository.findByRemisionFolioOrderByPerformedAtAsc(remisionFolio);
    }

    @Override
    public List<InventoryAuditLogEntity> findByPalletId(UUID palletId) {
        return jpaRepository.findByPalletIdOrderByPerformedAtAsc(palletId);
    }
}
