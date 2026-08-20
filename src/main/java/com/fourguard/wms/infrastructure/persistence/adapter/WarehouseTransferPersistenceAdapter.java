package com.fourguard.wms.infrastructure.persistence.adapter;

import com.fourguard.wms.domain.enums.TransferStatus;
import com.fourguard.wms.domain.ports.out.WarehouseTransferRepositoryPort;
import com.fourguard.wms.infrastructure.persistence.entity.WarehouseTransferEntity;
import com.fourguard.wms.infrastructure.persistence.repository.WarehouseTransferJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class WarehouseTransferPersistenceAdapter implements WarehouseTransferRepositoryPort {

    private final WarehouseTransferJpaRepository repository;

    @Override
    public Optional<WarehouseTransferEntity> findById(UUID id) {
        return repository.findById(id);
    }

    @Override
    public List<WarehouseTransferEntity> findByOrganizationId(UUID organizationId) {
        return repository.findByOrganizationIdOrderByCreatedAtDesc(organizationId);
    }

    @Override
    public List<WarehouseTransferEntity> findByOrganizationIdAndStatus(UUID organizationId, String status) {
        TransferStatus transferStatus = TransferStatus.valueOf(status);
        return repository.findByOrganizationIdAndStatusOrderByCreatedAtDesc(organizationId, transferStatus);
    }

    @Override
    public WarehouseTransferEntity save(WarehouseTransferEntity entity) {
        return repository.save(entity);
    }

    @Override
    public long nextFolioSequenceValue() {
        return repository.getNextFolioSequenceValue();
    }
}
