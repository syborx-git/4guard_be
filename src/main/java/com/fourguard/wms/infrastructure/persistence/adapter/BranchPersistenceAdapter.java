package com.fourguard.wms.infrastructure.persistence.adapter;

import com.fourguard.wms.domain.ports.out.BranchRepositoryPort;
import com.fourguard.wms.infrastructure.persistence.entity.BranchEntity;
import com.fourguard.wms.infrastructure.persistence.repository.BranchJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class BranchPersistenceAdapter implements BranchRepositoryPort {

    private final BranchJpaRepository repository;

    @Override
    @Cacheable(value = "catalogues", key = "'branch-' + #id", unless = "#result == null")
    public Optional<BranchEntity> findById(UUID id)                                      { return repository.findById(id); }

    @Override
    @Cacheable(value = "catalogues", key = "'branches-org-' + #orgId")
    public List<BranchEntity>     findByOrganizationId(UUID orgId)                       { return repository.findByOrganizationId(orgId); }

    @Override
    @CacheEvict(value = "catalogues", allEntries = true)
    public BranchEntity           save(BranchEntity branch)                              { return repository.save(branch); }

    @Override
    public boolean                existsByOrganizationIdAndCode(UUID orgId, String code) { return repository.existsByOrganizationIdAndCode(orgId, code); }

    @Override
    @Cacheable(value = "catalogues", key = "'branches-all'")
    public List<BranchEntity>     findAll()                                              { return repository.findAll(); }

    @Override
    @CacheEvict(value = "catalogues", allEntries = true)
    public void                   deleteById(UUID id)                                    { repository.deleteById(id); }
}
