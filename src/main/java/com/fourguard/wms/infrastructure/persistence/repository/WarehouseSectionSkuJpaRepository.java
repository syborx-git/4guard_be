package com.fourguard.wms.infrastructure.persistence.repository;

import com.fourguard.wms.infrastructure.persistence.entity.WarehouseSectionSkuEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface WarehouseSectionSkuJpaRepository extends JpaRepository<WarehouseSectionSkuEntity, UUID> {
    List<WarehouseSectionSkuEntity> findBySectionId(UUID sectionId);
    void deleteBySectionId(UUID sectionId);
    boolean existsBySectionIdAndSkuId(UUID sectionId, UUID skuId);
}
