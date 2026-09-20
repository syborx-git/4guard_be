package com.fourguard.wms.infrastructure.persistence.repository;

import com.fourguard.wms.infrastructure.persistence.entity.InventoryItemEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface InventoryItemJpaRepository extends JpaRepository<InventoryItemEntity, UUID> {

    Optional<InventoryItemEntity> findBySscc(String sscc);

    List<InventoryItemEntity> findByBranchId(UUID branchId);

    Page<InventoryItemEntity> findByBranchId(UUID branchId, Pageable pageable);

    List<InventoryItemEntity> findByLocationId(UUID locationId);

    /** FEFO query — uses idx_inventory_fefo index (state=30=AVAILABLE). */
    @Query("""
            SELECT i FROM InventoryItemEntity i
            WHERE i.sku.id = :skuId AND i.state = com.fourguard.wms.domain.enums.InventoryState.AVAILABLE
            ORDER BY i.expirationDate ASC NULLS LAST
            """)
    List<InventoryItemEntity> findAvailableBySkuOrderedByFefo(UUID skuId);

    /**
     * High-performance filtered query for available inventory batches (Outbound F03).
     * Eliminates N+1 queries via JOIN FETCH on sku, location, and client.
     */
    @Query("""
            SELECT i FROM InventoryItemEntity i
            JOIN FETCH i.sku s
            LEFT JOIN FETCH i.location l
            LEFT JOIN FETCH i.client c
            WHERE (i.state = com.fourguard.wms.domain.enums.InventoryState.AVAILABLE
                   OR i.state = com.fourguard.wms.domain.enums.InventoryState.EXPIRED)
              AND (:organizationId IS NULL OR i.organization.id = :organizationId)
              AND (:branchId IS NULL OR i.branch.id = :branchId)
              AND (:clientId IS NULL OR i.client.id = :clientId)
              AND (:skuId IS NULL OR s.id = :skuId)
              AND (:search IS NULL OR :search = ''
                   OR LOWER(i.sscc) LIKE LOWER(CONCAT('%', :search, '%'))
                   OR LOWER(i.externalUa) LIKE LOWER(CONCAT('%', :search, '%'))
                   OR LOWER(i.batchNumber) LIKE LOWER(CONCAT('%', :search, '%'))
                   OR LOWER(i.sapFolio) LIKE LOWER(CONCAT('%', :search, '%'))
                   OR LOWER(s.code) LIKE LOWER(CONCAT('%', :search, '%'))
                   OR LOWER(s.name) LIKE LOWER(CONCAT('%', :search, '%')))
            ORDER BY i.expirationDate ASC NULLS LAST, i.createdAt ASC
            """)
    List<InventoryItemEntity> findAvailableBatchesWithFilters(
            UUID organizationId,
            UUID branchId,
            UUID clientId,
            UUID skuId,
            String search);

    /**
     * Fast barcode / RF scanner lookup for a single pallet by SSCC or external UA.
     */
    @Query("""
            SELECT i FROM InventoryItemEntity i
            JOIN FETCH i.sku s
            LEFT JOIN FETCH i.location l
            LEFT JOIN FETCH i.client c
            WHERE (LOWER(i.sscc) = LOWER(:barcode) OR LOWER(i.externalUa) = LOWER(:barcode))
              AND (:organizationId IS NULL OR i.organization.id = :organizationId)
              AND (:branchId IS NULL OR i.branch.id = :branchId)
            """)
    Optional<InventoryItemEntity> findAvailableBySsccOrExternalUa(
            String barcode,
            UUID organizationId,
            UUID branchId);

    /**
     * Batch fetch for outbound creation — retrieves all selected items in a single round-trip.
     */
    @Query("""
            SELECT i FROM InventoryItemEntity i
            JOIN FETCH i.sku s
            LEFT JOIN FETCH i.location l
            LEFT JOIN FETCH i.client c
            WHERE i.id IN :ids
            """)
    List<InventoryItemEntity> findAllByIdInWithDetails(List<UUID> ids);

    /**
     * Bulk barcode lookup for mass scanning and multi-pallet validation.
     */
    @Query("""
            SELECT i FROM InventoryItemEntity i
            JOIN FETCH i.sku s
            LEFT JOIN FETCH i.location l
            LEFT JOIN FETCH i.client c
            WHERE (i.sscc IN :barcodes OR i.externalUa IN :barcodes)
              AND (:organizationId IS NULL OR i.organization.id = :organizationId)
              AND (:branchId IS NULL OR i.branch.id = :branchId)
            """)
    List<InventoryItemEntity> findByBarcodesIn(
            List<String> barcodes,
            UUID organizationId,
            UUID branchId);

    boolean existsBySscc(String sscc);

    /**
     * Bulk update sapFolio (remisionNo) in branch without loading all entities into memory.
     */
    @org.springframework.data.jpa.repository.Modifying
    @Query("UPDATE InventoryItemEntity i SET i.sapFolio = :newDoc WHERE i.branch.id = :branchId AND LOWER(i.sapFolio) = LOWER(:oldDoc)")
    int updateSapFolioInBranch(UUID branchId, String oldDoc, String newDoc);
}
