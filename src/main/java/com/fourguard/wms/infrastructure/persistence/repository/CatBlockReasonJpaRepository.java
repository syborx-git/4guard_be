package com.fourguard.wms.infrastructure.persistence.repository;

import com.fourguard.wms.infrastructure.persistence.entity.CatBlockReasonEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface CatBlockReasonJpaRepository extends JpaRepository<CatBlockReasonEntity, UUID> {
    List<CatBlockReasonEntity> findByIsActiveTrueOrderByDescriptionAsc();
}
