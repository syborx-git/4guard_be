package com.fourguard.wms.domain.ports.out;

import com.fourguard.wms.infrastructure.persistence.entity.CarrierEntity;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Port OUT — Carrier repository contract. */
public interface CarrierRepositoryPort {
    Optional<CarrierEntity> findById(UUID id);
    List<CarrierEntity>     findByOrganizationId(UUID organizationId);
    CarrierEntity           save(CarrierEntity carrier);
    void                   deleteById(UUID id);
    List<CarrierEntity>     findAll();
    boolean                 existsByTaxId(String taxId);
    boolean                 existsByTaxIdAndIdNot(String taxId, UUID excludeId);
    boolean                 existsByOrganizationIdAndTaxId(UUID organizationId, String taxId);
    boolean                 existsByOrganizationIdAndTaxIdAndIdNot(UUID organizationId, String taxId, UUID excludeId);
}
