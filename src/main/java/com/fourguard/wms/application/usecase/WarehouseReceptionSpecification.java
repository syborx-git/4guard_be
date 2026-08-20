package com.fourguard.wms.application.usecase;

import com.fourguard.wms.domain.enums.ReceptionStatus;
import com.fourguard.wms.infrastructure.persistence.entity.ClientEntity;
import com.fourguard.wms.infrastructure.persistence.entity.WarehouseReceptionEntity;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class WarehouseReceptionSpecification {

    private WarehouseReceptionSpecification() {}

    public static Specification<WarehouseReceptionEntity> withFilters(
            UUID orgId, UUID branchId, ReceptionStatus status, String search) {
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
