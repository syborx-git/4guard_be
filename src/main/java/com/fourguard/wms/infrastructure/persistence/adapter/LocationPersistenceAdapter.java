package com.fourguard.wms.infrastructure.persistence.adapter;

import com.fourguard.wms.domain.ports.out.LocationRepositoryPort;
import com.fourguard.wms.infrastructure.persistence.entity.LocationEntity;
import com.fourguard.wms.infrastructure.persistence.repository.LocationJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class LocationPersistenceAdapter implements LocationRepositoryPort {
    private final LocationJpaRepository repository;

    @Override public Optional<LocationEntity> findById(UUID id)                { return repository.findById(id); }
    @Override public List<LocationEntity>     findByBranchId(UUID bid)         { return repository.findByBranchId(bid); }
    @Override public List<LocationEntity>     findAvailableByBranchId(UUID bid){ return repository.findByBranchIdAndIsBlockedFalse(bid); }
    @Override public Optional<LocationEntity> findByBranchIdAndCode(UUID bid, String code) { return repository.findByBranchIdAndCode(bid, code); }
    @Override public Optional<LocationEntity> findFirstByCode(String code)     { return repository.findFirstByCode(code); }
    @Override public LocationEntity           save(LocationEntity l)           { return repository.save(l); }
    @Override public void                     deleteById(UUID id)              { repository.deleteById(id); }
    @Override public List<LocationEntity>     findAll()                        { return repository.findAll(); }
    @Override public List<LocationEntity>     findBySectionId(UUID sid)        { return repository.findBySectionId(sid); }


    @Override public boolean existsByCodeAndIdNot(String code, UUID excludeId) {
        return repository.existsByCodeAndIdNot(code, excludeId);
    }
}


