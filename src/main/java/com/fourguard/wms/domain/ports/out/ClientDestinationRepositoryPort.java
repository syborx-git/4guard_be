package com.fourguard.wms.domain.ports.out;

import com.fourguard.wms.infrastructure.persistence.entity.ClientDestinationEntity;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Puerto de salida — Repositorio de Destinos Físicos de Clientes. */
public interface ClientDestinationRepositoryPort {
    List<ClientDestinationEntity>    findByClientId(UUID clientId);
    Optional<ClientDestinationEntity> findById(UUID id);
    ClientDestinationEntity          save(ClientDestinationEntity destination);
    void                             deleteById(UUID id);
    boolean                          existsByClientIdAndDestinationCode(UUID clientId, String destinationCode);
    boolean                          existsByClientIdAndDestinationCodeAndIdNot(UUID clientId, String code, UUID id);
}
