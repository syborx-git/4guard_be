package com.fourguard.wms.application.usecase;

import com.fourguard.wms.domain.enums.TransferStatus;
import com.fourguard.wms.infrastructure.persistence.entity.LocationEntity;
import com.fourguard.wms.infrastructure.persistence.entity.WarehouseTransferEntity;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class WarehouseTransferSpecification {

    private WarehouseTransferSpecification() {}

    public static Specification<WarehouseTransferEntity> withFilters(
            UUID orgId, UUID branchId, TransferStatus status, String search) {
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
            if (search != null && !search.isBlank()) {
                String pattern = "%" + search.toLowerCase().trim() + "%";
                Predicate folioMatch = cb.like(cb.lower(root.get("folio")), pattern);

                Join<WarehouseTransferEntity, LocationEntity> originJoin = root.join("originLocation", JoinType.LEFT);
                Predicate originMatch = cb.like(cb.lower(originJoin.get("code")), pattern);

                Join<WarehouseTransferEntity, LocationEntity> destJoin = root.join("destinationLocation", JoinType.LEFT);
                Predicate destMatch = cb.like(cb.lower(destJoin.get("code")), pattern);

                predicates.add(cb.or(folioMatch, originMatch, destMatch));
            }

            if (query != null) {
                query.orderBy(cb.desc(root.get("createdAt")));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
