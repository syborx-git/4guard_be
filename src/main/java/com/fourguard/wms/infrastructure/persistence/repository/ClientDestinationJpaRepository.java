package com.fourguard.wms.infrastructure.persistence.repository;

import com.fourguard.wms.infrastructure.persistence.entity.ClientDestinationEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

/** Repositorio JPA — Destinos Físicos de Clientes (wms.client_destinations). */
@Repository
public interface ClientDestinationJpaRepository extends JpaRepository<ClientDestinationEntity, UUID> {
    List<ClientDestinationEntity> findByClientId(UUID clientId);
    List<ClientDestinationEntity> findByClientIdAndStatus(UUID clientId, String status);
    boolean existsByClientIdAndDestinationCode(UUID clientId, String destinationCode);
    boolean existsByClientIdAndDestinationCodeAndIdNot(UUID clientId, String destinationCode, UUID id);
}
