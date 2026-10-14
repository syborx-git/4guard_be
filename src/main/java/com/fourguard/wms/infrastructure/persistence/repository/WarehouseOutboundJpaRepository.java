package com.fourguard.wms.infrastructure.persistence.repository;

import com.fourguard.wms.domain.enums.OutboundStatus;
import com.fourguard.wms.infrastructure.persistence.entity.WarehouseOutboundEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface WarehouseOutboundJpaRepository extends
        JpaRepository<WarehouseOutboundEntity, UUID>,
        JpaSpecificationExecutor<WarehouseOutboundEntity> {

    List<WarehouseOutboundEntity> findByOrganizationIdOrderByCreatedAtDesc(UUID organizationId);

    List<WarehouseOutboundEntity> findByOrganizationIdAndStatusOrderByCreatedAtDesc(UUID organizationId, OutboundStatus status);

    Optional<WarehouseOutboundEntity> findByFolio(String folio);

    List<WarehouseOutboundEntity> findByRemisionNoIgnoreCase(String remisionNo);

    @Query("""
        SELECT DISTINCT o FROM WarehouseOutboundEntity o
        LEFT JOIN FETCH o.items i
        LEFT JOIN FETCH i.item it
        LEFT JOIN FETCH it.sku s
        LEFT JOIN FETCH it.client c
        LEFT JOIN FETCH it.location loc
        LEFT JOIN FETCH o.client
        LEFT JOIN FETCH o.carrier
        LEFT JOIN FETCH o.ramp
        LEFT JOIN FETCH o.forkliftOperator
        LEFT JOIN FETCH o.destination
        LEFT JOIN FETCH o.preCheckin
        WHERE o.id = :id
    """)
    Optional<WarehouseOutboundEntity> findByIdWithDetails(@org.springframework.data.repository.query.Param("id") UUID id);

    @Query("""
        SELECT DISTINCT o FROM WarehouseOutboundEntity o
        LEFT JOIN FETCH o.items i
        WHERE (:organizationId IS NULL OR o.organization.id = :organizationId)
          AND (UPPER(o.folio) = UPPER(:query)
           OR UPPER(o.remisionNo) = UPPER(:query)
           OR UPPER(i.lotNumber) = UPPER(:query)
           OR UPPER(i.palletCode) = UPPER(:query))
        ORDER BY o.createdAt DESC
    """)
    List<WarehouseOutboundEntity> searchOutboundsForReturn(
        @org.springframework.data.repository.query.Param("organizationId") UUID organizationId,
        @org.springframework.data.repository.query.Param("query") String query
    );

    @Query(value = "SELECT nextval('wms.seq_outbound_folio')", nativeQuery = true)
    long getNextFolioSequenceValue();

    @Query(value = "SELECT wms.fn_next_client_outbound_seq(:clientId)", nativeQuery = true)
    long getNextClientFolioSequenceValue(@org.springframework.data.repository.query.Param("clientId") UUID clientId);
}
