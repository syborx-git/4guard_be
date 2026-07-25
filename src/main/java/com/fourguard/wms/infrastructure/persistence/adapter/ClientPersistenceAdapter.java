package com.fourguard.wms.infrastructure.persistence.adapter;

import com.fourguard.wms.domain.ports.out.ClientRepositoryPort;
import com.fourguard.wms.infrastructure.persistence.entity.ClientEntity;
import com.fourguard.wms.infrastructure.persistence.repository.ClientJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class ClientPersistenceAdapter implements ClientRepositoryPort {
    private final ClientJpaRepository repository;

    @Override public Optional<ClientEntity> findById(UUID id)                { return repository.findById(id); }
    @Override public List<ClientEntity>     findByOrganizationId(UUID orgId) { return repository.findByOrganizationId(orgId); }
    @Override public ClientEntity           save(ClientEntity c)             { return repository.save(c); }
    @Override public void                   deleteById(UUID id)              { repository.deleteById(id); }
    @Override public List<ClientEntity>     findAll()                        { return repository.findAll(); }

    @Override
    public boolean existsByOrganizationIdAndTaxId(UUID organizationId, String taxId) {
        return repository.existsByOrganizationIdAndTaxIdIgnoreCase(organizationId, taxId);
    }

    @Override
    public boolean existsByOrganizationIdAndTaxIdAndIdNot(UUID organizationId, String taxId, UUID id) {
        return repository.existsByOrganizationIdAndTaxIdIgnoreCaseAndIdNot(organizationId, taxId, id);
    }
}
