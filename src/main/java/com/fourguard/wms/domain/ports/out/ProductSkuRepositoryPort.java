package com.fourguard.wms.domain.ports.out;

import com.fourguard.wms.infrastructure.persistence.entity.ProductSkuEntity;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Port OUT — ProductSku repository contract. */
public interface ProductSkuRepositoryPort {
    Optional<ProductSkuEntity> findById(UUID id);
    Optional<ProductSkuEntity> findByClientIdAndCode(UUID clientId, String code);
    List<ProductSkuEntity>     findByClientId(UUID clientId);
    List<ProductSkuEntity>     findByClientIdActive(UUID clientId);
    ProductSkuEntity           save(ProductSkuEntity sku);
    void                       deleteById(UUID id);
    List<ProductSkuEntity>     findAll();
    List<ProductSkuEntity>     findAllActive();
}
