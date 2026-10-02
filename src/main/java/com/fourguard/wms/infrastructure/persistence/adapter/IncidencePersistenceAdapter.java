package com.fourguard.wms.infrastructure.persistence.adapter;

import com.fourguard.wms.domain.ports.out.IncidenceRepositoryPort;
import com.fourguard.wms.infrastructure.persistence.entity.IncidenceEntity;
import com.fourguard.wms.infrastructure.persistence.repository.IncidenceJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class IncidencePersistenceAdapter implements IncidenceRepositoryPort {
    private final IncidenceJpaRepository repository;

    @Override public Optional<IncidenceEntity> findById(UUID id)           { return repository.findById(Objects.requireNonNull(id)); }
    @Override public Optional<IncidenceEntity> findByFolio(Integer folio)  { return repository.findByFolio(folio); }
    @Override public List<IncidenceEntity>     findByItemId(UUID itemId)   { return repository.findByItemId(itemId); }

    @Override
    public IncidenceEntity save(IncidenceEntity incidence) {
        // saveAndFlush + findById ensures the DB-generated folio is populated
        IncidenceEntity saved = repository.saveAndFlush(Objects.requireNonNull(incidence));
        return repository.findById(Objects.requireNonNull(saved.getId())).orElse(saved);
    }
}
