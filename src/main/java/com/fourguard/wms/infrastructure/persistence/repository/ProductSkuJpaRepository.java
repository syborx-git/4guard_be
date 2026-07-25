package com.fourguard.wms.infrastructure.persistence.repository;

import com.fourguard.wms.infrastructure.persistence.entity.ProductSkuEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ProductSkuJpaRepository extends JpaRepository<ProductSkuEntity, UUID> {
    List<ProductSkuEntity> findByClientId(UUID clientId);
    List<ProductSkuEntity> findByClientIdAndIsDeletedFalse(UUID clientId);
    List<ProductSkuEntity> findByIsDeletedFalse();
    Optional<ProductSkuEntity> findByClientIdAndCode(UUID clientId, String code);
    Optional<ProductSkuEntity> findByClientIdAndCodeAndIsDeletedFalse(UUID clientId, String code);
    boolean existsByClientIdAndCode(UUID clientId, String code);
}
