package com.fourguard.wms.infrastructure.persistence.adapter;

import com.fourguard.wms.domain.ports.out.ProductSkuRepositoryPort;
import com.fourguard.wms.infrastructure.persistence.entity.ProductSkuEntity;
import com.fourguard.wms.infrastructure.persistence.repository.ProductSkuJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class ProductSkuPersistenceAdapter implements ProductSkuRepositoryPort {
    private final ProductSkuJpaRepository repository;

    @Override
    @Cacheable(value = "catalogues", key = "'sku-' + #id", unless = "#result == null")
    public Optional<ProductSkuEntity> findById(UUID id)                              { return repository.findById(id); }

    @Override
    @Cacheable(value = "catalogues", key = "'sku-client-code-' + #cid + '-' + #code", unless = "#result == null")
    public Optional<ProductSkuEntity> findByClientIdAndCode(UUID cid, String code)   { return repository.findByClientIdAndCode(cid, code); }

    @Override
    @Cacheable(value = "catalogues", key = "'skus-client-' + #cid")
    public List<ProductSkuEntity>     findByClientId(UUID cid)                       { return repository.findByClientId(cid); }

    @Override
    @CacheEvict(value = "catalogues", allEntries = true)
    public ProductSkuEntity           save(ProductSkuEntity sku)                     { return repository.save(sku); }

    @Override
    @CacheEvict(value = "catalogues", allEntries = true)
    public void                       deleteById(UUID id)                            { repository.deleteById(id); }

    @Override
    @Cacheable(value = "catalogues", key = "'skus-all'")
    public List<ProductSkuEntity>     findAll()                                      { return repository.findAll(); }
}
