package com.fourguard.wms.infrastructure.persistence.adapter;

import com.fourguard.wms.domain.ports.out.ClientDestinationRepositoryPort;
import com.fourguard.wms.infrastructure.persistence.entity.ClientDestinationEntity;
import com.fourguard.wms.infrastructure.persistence.repository.ClientDestinationJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Adaptador de persistencia para Destinos Físicos de Clientes. */
@Component
@RequiredArgsConstructor
public class ClientDestinationPersistenceAdapter implements ClientDestinationRepositoryPort {

    private final ClientDestinationJpaRepository repository;

    @Override
    public List<ClientDestinationEntity> findByClientId(UUID clientId) {
        return repository.findByClientId(clientId);
    }

    @Override
    public Optional<ClientDestinationEntity> findById(UUID id) {
        return repository.findById(id);
    }

    @Override
    public ClientDestinationEntity save(ClientDestinationEntity destination) {
        return repository.save(destination);
    }

    @Override
    public void deleteById(UUID id) {
        repository.deleteById(id);
    }

    @Override
    public boolean existsByClientIdAndDestinationCode(UUID clientId, String destinationCode) {
        return repository.existsByClientIdAndDestinationCode(clientId, destinationCode);
    }

    @Override
    public boolean existsByClientIdAndDestinationCodeAndIdNot(UUID clientId, String code, UUID id) {
        return repository.existsByClientIdAndDestinationCodeAndIdNot(clientId, code, id);
    }
}
