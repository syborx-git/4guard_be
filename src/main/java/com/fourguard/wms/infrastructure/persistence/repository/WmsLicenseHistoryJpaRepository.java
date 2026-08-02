package com.fourguard.wms.infrastructure.persistence.repository;

import com.fourguard.wms.infrastructure.persistence.entity.WmsLicenseHistoryEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface WmsLicenseHistoryJpaRepository extends JpaRepository<WmsLicenseHistoryEntity, UUID> {
    List<WmsLicenseHistoryEntity> findByLicenseIdOrderByPerformedAtDesc(UUID licenseId);
}
