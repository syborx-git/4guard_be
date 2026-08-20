package com.fourguard.wms.infrastructure.persistence.repository;

import com.fourguard.wms.infrastructure.persistence.entity.ClientEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Repositorio JPA para Clientes Depositantes (wms.clients). */
@Repository
public interface ClientJpaRepository extends JpaRepository<ClientEntity, UUID> {
    List<ClientEntity>     findByOrganizationId(UUID organizationId);
    Optional<ClientEntity> findByOrganizationIdAndExternalId(UUID organizationId, String externalId);

    // Unicidad por Tax ID
    boolean existsByOrganizationIdAndTaxIdIgnoreCase(UUID organizationId, String taxId);
    boolean existsByOrganizationIdAndTaxIdIgnoreCaseAndIdNot(UUID organizationId, String taxId, UUID id);

    // Unicidad por External ID
    boolean existsByOrganizationIdAndExternalIdIgnoreCase(UUID organizationId, String externalId);
    boolean existsByOrganizationIdAndExternalIdIgnoreCaseAndIdNot(UUID organizationId, String externalId, UUID id);
}
