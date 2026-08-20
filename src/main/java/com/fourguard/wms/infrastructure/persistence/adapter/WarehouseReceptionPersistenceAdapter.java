package com.fourguard.wms.infrastructure.persistence.adapter;

import com.fourguard.wms.domain.enums.ReceptionStatus;
import com.fourguard.wms.domain.ports.out.WarehouseReceptionRepositoryPort;
import com.fourguard.wms.infrastructure.persistence.entity.WarehouseReceptionEntity;
import com.fourguard.wms.infrastructure.persistence.repository.WarehouseReceptionJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class WarehouseReceptionPersistenceAdapter implements WarehouseReceptionRepositoryPort {

    private final WarehouseReceptionJpaRepository repository;

    @Override
    public Optional<WarehouseReceptionEntity> findById(UUID id) {
        return repository.findById(id);
    }

    @Override
    public List<WarehouseReceptionEntity> findByOrganizationId(UUID organizationId) {
        return repository.findByOrganizationIdOrderByCreatedAtDesc(organizationId);
    }

    @Override
    public List<WarehouseReceptionEntity> findByOrganizationIdAndBranchId(UUID organizationId, UUID branchId) {
        return repository.findByOrganizationIdAndBranchIdOrderByCreatedAtDesc(organizationId, branchId);
    }

    @Override
    public List<WarehouseReceptionEntity> findByOrganizationIdAndStatus(UUID organizationId, String status) {
        ReceptionStatus recStatus = ReceptionStatus.valueOf(status);
        return repository.findByOrganizationIdAndStatusOrderByCreatedAtDesc(organizationId, recStatus);
    }

    @Override
    public Optional<WarehouseReceptionEntity> findByFolio(String folio) {
        return repository.findByFolio(folio);
    }

    @Override
    public boolean existsByOrganizationIdAndDocNumber(UUID organizationId, String docNumber) {
        return repository.existsByOrganizationIdAndDocNumber(organizationId, docNumber);
    }

    @Override
    public WarehouseReceptionEntity save(WarehouseReceptionEntity entity) {
        return repository.save(entity);
    }

    @Override
    public long nextFolioSequenceValue() {
        return repository.getNextFolioSequenceValue();
    }
}
