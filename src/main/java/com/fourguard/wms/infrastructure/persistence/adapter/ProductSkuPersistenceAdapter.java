package com.fourguard.wms.infrastructure.persistence.adapter;

import com.fourguard.wms.domain.ports.out.ProductSkuRepositoryPort;
import com.fourguard.wms.infrastructure.persistence.entity.ProductSkuEntity;
import com.fourguard.wms.infrastructure.persistence.repository.ProductSkuJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class ProductSkuPersistenceAdapter implements ProductSkuRepositoryPort {
    private final ProductSkuJpaRepository repository;

    @Override public Optional<ProductSkuEntity> findById(UUID id)                              { return repository.findById(id); }
    @Override public Optional<ProductSkuEntity> findByClientIdAndCode(UUID cid, String code)   { return repository.findByClientIdAndCodeAndIsDeletedFalse(cid, code); }
    @Override public List<ProductSkuEntity>     findByClientId(UUID cid)                       { return repository.findByClientId(cid); }
    @Override public List<ProductSkuEntity>     findByClientIdActive(UUID cid)                 { return repository.findByClientIdAndIsDeletedFalse(cid); }
    @Override public ProductSkuEntity           save(ProductSkuEntity sku)                     { return repository.save(sku); }
    @Override public void                       deleteById(UUID id)                            { repository.deleteById(id); }
    @Override public List<ProductSkuEntity>     findAll()                                      { return repository.findAll(); }
    @Override public List<ProductSkuEntity>     findAllActive()                                { return repository.findByIsDeletedFalse(); }
}
