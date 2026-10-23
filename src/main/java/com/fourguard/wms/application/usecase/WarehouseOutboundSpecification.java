package com.fourguard.wms.application.usecase;

import com.fourguard.wms.domain.enums.MovementScope;
import com.fourguard.wms.domain.enums.OutboundStatus;
import com.fourguard.wms.infrastructure.persistence.entity.ClientEntity;
import com.fourguard.wms.infrastructure.persistence.entity.WarehouseOutboundEntity;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class WarehouseOutboundSpecification {

    private WarehouseOutboundSpecification() {}

    public static Specification<WarehouseOutboundEntity> withFilters(
            UUID orgId, UUID branchId, OutboundStatus status, String search) {
        return withFilters(orgId, branchId, status, search, null, MovementScope.ALL);
    }

    public static Specification<WarehouseOutboundEntity> withHistoryFilters(
            UUID orgId, UUID branchId, OutboundStatus status, String search,
            UUID shiftSessionId, OffsetDateTime from, OffsetDateTime to, UUID clientId) {
        Specification<WarehouseOutboundEntity> base = withFilters(
                orgId, branchId, status, search, shiftSessionId, MovementScope.ALL);

        return base.and((root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (from != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("createdAt"), from));
            }
            if (to != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("createdAt"), to));
            }
            if (clientId != null) {
                predicates.add(cb.equal(root.get("client").get("id"), clientId));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        });
    }

    public static Specification<WarehouseOutboundEntity> withFilters(
            UUID orgId, UUID branchId, OutboundStatus status, String search,
            UUID shiftSessionId, MovementScope scope) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (orgId != null) {
                predicates.add(cb.equal(root.get("organization").get("id"), orgId));
            }
            if (branchId != null) {
                predicates.add(cb.equal(root.get("branch").get("id"), branchId));
            }
            if (status != null) {
                predicates.add(cb.equal(root.get("status"), status));
            }
            if (shiftSessionId != null) {
                predicates.add(cb.equal(root.get("shiftSessionId"), shiftSessionId));
            }
            if (scope == MovementScope.CURRENT) {
                predicates.add(root.get("status").in(List.of(
                        OutboundStatus.DRAFT,
                        OutboundStatus.REGISTERED,
                        OutboundStatus.ASSIGNED,
                        OutboundStatus.IN_PROGRESS,
                        OutboundStatus.LOADED)));
            } else if (scope == MovementScope.HISTORY) {
                predicates.add(root.get("status").in(List.of(
                        OutboundStatus.COMPLETED,
                        OutboundStatus.CANCELLED)));
            }
            if (search != null && !search.isBlank()) {
                String pattern = "%" + search.toLowerCase().trim() + "%";
                Predicate folioMatch = cb.like(cb.lower(root.get("folio")), pattern);
                Predicate remisionMatch = cb.like(cb.lower(root.get("remisionNo")), pattern);
                Predicate driverMatch = cb.like(cb.lower(root.get("driverName")), pattern);

                Join<WarehouseOutboundEntity, ClientEntity> clientJoin = root.join("client", JoinType.LEFT);
                Predicate clientMatch = cb.like(cb.lower(clientJoin.get("name")), pattern);

                predicates.add(cb.or(folioMatch, remisionMatch, driverMatch, clientMatch));
            }

            if (query != null) {
                query.orderBy(cb.desc(root.get("createdAt")));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
