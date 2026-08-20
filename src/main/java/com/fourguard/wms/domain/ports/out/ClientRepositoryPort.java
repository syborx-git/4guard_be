package com.fourguard.wms.domain.ports.out;

import com.fourguard.wms.infrastructure.persistence.entity.ClientEntity;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Puerto de salida — Repositorio de Clientes Depositantes / Owners 3PL. */
public interface ClientRepositoryPort {
    Optional<ClientEntity> findById(UUID id);
    List<ClientEntity>     findByOrganizationId(UUID organizationId);
    ClientEntity           save(ClientEntity client);
    void                   deleteById(UUID id);
    List<ClientEntity>     findAll();

    // Validaciones de unicidad — RFC / Tax ID (RN-CLI-001)
    boolean existsByOrganizationIdAndTaxId(UUID organizationId, String taxId);
    boolean existsByOrganizationIdAndTaxIdAndIdNot(UUID organizationId, String taxId, UUID id);

    // Validaciones de unicidad — External ID / Código ERP
    boolean existsByOrganizationIdAndExternalId(UUID organizationId, String externalId);
    boolean existsByOrganizationIdAndExternalIdAndIdNot(UUID organizationId, String externalId, UUID id);
}
