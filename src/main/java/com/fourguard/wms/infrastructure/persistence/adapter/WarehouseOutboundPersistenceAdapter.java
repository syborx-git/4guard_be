package com.fourguard.wms.infrastructure.persistence.adapter;

import com.fourguard.wms.domain.enums.OutboundStatus;
import com.fourguard.wms.domain.ports.out.WarehouseOutboundRepositoryPort;
import com.fourguard.wms.infrastructure.persistence.entity.WarehouseOutboundEntity;
import com.fourguard.wms.infrastructure.persistence.repository.WarehouseOutboundJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class WarehouseOutboundPersistenceAdapter implements WarehouseOutboundRepositoryPort {

    private final WarehouseOutboundJpaRepository repository;

    @Override
    public Optional<WarehouseOutboundEntity> findById(UUID id) {
        return repository.findById(id);
    }

    @Override
    public List<WarehouseOutboundEntity> findByOrganizationId(UUID organizationId) {
        return repository.findByOrganizationIdOrderByCreatedAtDesc(organizationId);
    }

    @Override
    public List<WarehouseOutboundEntity> findByOrganizationIdAndStatus(UUID organizationId, String status) {
        OutboundStatus outboundStatus = OutboundStatus.valueOf(status);
        return repository.findByOrganizationIdAndStatusOrderByCreatedAtDesc(organizationId, outboundStatus);
    }

    @Override
    public WarehouseOutboundEntity save(WarehouseOutboundEntity entity) {
        return repository.save(entity);
    }

    @Override
    public long nextFolioSequenceValue() {
        return repository.getNextFolioSequenceValue();
    }
}
