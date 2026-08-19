package com.fourguard.wms.infrastructure.persistence.repository;

import com.fourguard.wms.infrastructure.persistence.entity.ForkliftOperatorEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Spring Data JPA Repository for {@link ForkliftOperatorEntity} (HU-142).
 * All queries respect the soft-delete flag {@code is_deleted = false}.
 */
public interface ForkliftOperatorJpaRepository extends JpaRepository<ForkliftOperatorEntity, UUID> {

    /** Find an active (non-deleted) operator by its UUID. */
    Optional<ForkliftOperatorEntity> findByIdAndIsDeletedFalse(UUID id);

    /** List all active operators within an organization. */
    List<ForkliftOperatorEntity> findByOrganizationIdAndIsDeletedFalse(UUID organizationId);

    /** Check code uniqueness within an organization across ALL records (matches DB unique constraint). */
    boolean existsByOrganizationIdAndCode(UUID organizationId, String code);

    /** Check code uniqueness excluding the record being updated. */
    boolean existsByOrganizationIdAndCodeAndIdNot(UUID organizationId, String code, UUID excludeId);

    /** Check DC-3 license uniqueness within an organization across ALL records (matches DB unique constraint). */
    boolean existsByOrganizationIdAndLicenseNumberDc3IgnoreCase(UUID organizationId, String licenseNumberDc3);

    /** Check DC-3 license uniqueness excluding the record being updated. */
    boolean existsByOrganizationIdAndLicenseNumberDc3IgnoreCaseAndIdNot(UUID organizationId, String licenseNumberDc3, UUID excludeId);

    /** Soft delete: set is_deleted = true and record who did it. */
    @Modifying
    @Query("""
        UPDATE ForkliftOperatorEntity f
        SET    f.isDeleted  = true,
               f.updatedAt  = :now,
               f.updatedBy  = :deletedBy
        WHERE  f.id = :id AND f.isDeleted = false
    """)
    void softDeleteById(@Param("id") UUID id, @Param("deletedBy") String deletedBy, @Param("now") OffsetDateTime now);

    /** Find all active operators — used for admin-level exports. */
    List<ForkliftOperatorEntity> findAllByIsDeletedFalse();
}
