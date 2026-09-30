package com.fourguard.wms.infrastructure.persistence.repository;

import com.fourguard.wms.infrastructure.persistence.entity.WarehouseReceptionLotEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface WarehouseReceptionLotJpaRepository extends JpaRepository<WarehouseReceptionLotEntity, UUID> {

    List<WarehouseReceptionLotEntity> findByReceptionIdOrderByCreatedAtAsc(UUID receptionId);

    Optional<WarehouseReceptionLotEntity> findByReceptionIdAndId(UUID receptionId, UUID id);

    Optional<WarehouseReceptionLotEntity> findByReceptionIdAndLotNumber(UUID receptionId, String lotNumber);

    boolean existsByReceptionIdAndLotNumber(UUID receptionId, String lotNumber);

    void deleteByReceptionIdAndId(UUID receptionId, UUID id);
}
