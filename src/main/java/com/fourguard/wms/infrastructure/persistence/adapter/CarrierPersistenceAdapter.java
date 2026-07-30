package com.fourguard.wms.infrastructure.persistence.adapter;

import com.fourguard.wms.domain.ports.out.CarrierRepositoryPort;
import com.fourguard.wms.infrastructure.persistence.entity.CarrierEntity;
import com.fourguard.wms.infrastructure.persistence.repository.CarrierJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class CarrierPersistenceAdapter implements CarrierRepositoryPort {
    private final CarrierJpaRepository repository;

    @Override public Optional<CarrierEntity> findById(UUID id)                { return repository.findById(id); }
    @Override public List<CarrierEntity>     findByOrganizationId(UUID orgId) { return repository.findByOrganizationId(orgId); }
    @Override public CarrierEntity           save(CarrierEntity c)             { return repository.save(c); }
    @Override public void                   deleteById(UUID id)              { repository.deleteById(id); }
    @Override public List<CarrierEntity>     findAll()                        { return repository.findAll(); }

    @Override
    public boolean existsByTaxId(String taxId) {
        return repository.existsByTaxIdIgnoreCase(taxId);
    }

    @Override
    public boolean existsByTaxIdAndIdNot(String taxId, UUID excludeId) {
        return repository.existsByTaxIdIgnoreCaseAndIdNot(taxId, excludeId);
    }

    @Override
    public boolean existsByOrganizationIdAndTaxId(UUID organizationId, String taxId) {
        return repository.existsByOrganizationIdAndTaxIdIgnoreCase(organizationId, taxId);
    }

    @Override
    public boolean existsByOrganizationIdAndTaxIdAndIdNot(UUID organizationId, String taxId, UUID excludeId) {
        return repository.existsByOrganizationIdAndTaxIdIgnoreCaseAndIdNot(organizationId, taxId, excludeId);
    }
}
