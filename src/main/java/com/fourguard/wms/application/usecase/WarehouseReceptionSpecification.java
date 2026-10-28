package com.fourguard.wms.application.usecase;

import com.fourguard.wms.domain.enums.MovementScope;
import com.fourguard.wms.domain.enums.ReceptionStatus;
import com.fourguard.wms.infrastructure.persistence.entity.ClientEntity;
import com.fourguard.wms.infrastructure.persistence.entity.WarehouseReceptionEntity;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class WarehouseReceptionSpecification {

    private WarehouseReceptionSpecification() {}

    public static Specification<WarehouseReceptionEntity> withFilters(
            UUID orgId, UUID branchId, ReceptionStatus status, String search) {
        return withFilters(orgId, branchId, status, search, null, MovementScope.ALL, null);
    }

    public static Specification<WarehouseReceptionEntity> withHistoryFilters(
            UUID orgId, UUID branchId, ReceptionStatus status, String search,
            UUID shiftSessionId, OffsetDateTime from, OffsetDateTime to, UUID clientId) {
        Specification<WarehouseReceptionEntity> base = withFilters(
                orgId, branchId, status, search, shiftSessionId, MovementScope.ALL, null);

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

    public static Specification<WarehouseReceptionEntity> withFilters(
            UUID orgId, UUID branchId, ReceptionStatus status, String search,
            UUID shiftSessionId, MovementScope scope) {
        return withFilters(orgId, branchId, status, search, shiftSessionId, scope, null);
    }

    public static Specification<WarehouseReceptionEntity> withFilters(
            UUID orgId, UUID branchId, ReceptionStatus status, String search,
            UUID shiftSessionId, MovementScope scope, UUID forkliftOperatorId) {
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
            if (forkliftOperatorId != null) {
                predicates.add(cb.equal(root.get("forkliftOperator").get("id"), forkliftOperatorId));
            }
            if (scope == MovementScope.CURRENT) {
                predicates.add(root.get("status").in(List.of(
                        ReceptionStatus.REGISTERED,
                        ReceptionStatus.ASSIGNED,
                        ReceptionStatus.IN_PROGRESS,
                        ReceptionStatus.DISCHARGED)));
            } else if (scope == MovementScope.HISTORY) {
                predicates.add(root.get("status").in(List.of(
                        ReceptionStatus.COMPLETED,
                        ReceptionStatus.CANCELLED)));
            }
            if (search != null && !search.isBlank()) {
                String pattern = "%" + search.toLowerCase().trim() + "%";
                Predicate folioMatch = cb.like(cb.lower(root.get("folio")), pattern);
                Predicate docMatch = cb.like(cb.lower(root.get("docNumber")), pattern);
                Predicate driverMatch = cb.like(cb.lower(root.get("driverName")), pattern);

                Join<WarehouseReceptionEntity, ClientEntity> clientJoin = root.join("client", JoinType.LEFT);
                Predicate clientMatch = cb.like(cb.lower(clientJoin.get("name")), pattern);

                predicates.add(cb.or(folioMatch, docMatch, driverMatch, clientMatch));
            }

            if (query != null) {
                query.orderBy(cb.desc(root.get("createdAt")));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
