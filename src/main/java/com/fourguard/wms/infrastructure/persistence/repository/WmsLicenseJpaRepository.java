package com.fourguard.wms.infrastructure.persistence.repository;

import com.fourguard.wms.domain.enums.LicenseAdminStatus;
import com.fourguard.wms.infrastructure.persistence.entity.WmsLicenseEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface WmsLicenseJpaRepository extends JpaRepository<WmsLicenseEntity, UUID> {
    List<WmsLicenseEntity> findByOrganizationId(UUID organizationId);
    boolean existsByOrganizationIdAndAdminStatus(UUID organizationId, LicenseAdminStatus adminStatus);
}
