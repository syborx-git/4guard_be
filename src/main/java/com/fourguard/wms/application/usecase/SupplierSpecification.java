package com.fourguard.wms.application.usecase;

import com.fourguard.wms.application.dto.request.SupplierFilterRequest;
import com.fourguard.wms.infrastructure.persistence.entity.SupplierEntity;
import jakarta.persistence.criteria.*;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Factory class for building dynamic JPA Specifications for SupplierEntity queries.
 * All built specs always include is_deleted = false as a base predicate.
 */
public class SupplierSpecification {

    private SupplierSpecification() {}

    /**
     * Builds a combined Specification from the given filter request.
     * Every predicate is ANDed together.
     */
    public static Specification<SupplierEntity> fromFilter(SupplierFilterRequest filter, UUID resolvedClientUuid, UUID resolvedWarehouseUuid) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            // Always exclude soft-deleted records
            predicates.add(cb.isFalse(root.get("isDeleted")));

            // Organization filter (always applied — multi-tenant safety)
            if (filter.getOrganizationId() != null) {
                predicates.add(cb.equal(root.get("organization").get("id"), filter.getOrganizationId()));
            }

            // Status filter
            if (filter.getStatus() != null && !filter.getStatus().isBlank()) {
                predicates.add(cb.equal(cb.upper(root.get("status").as(String.class)),
                        filter.getStatus().toUpperCase()));
            }

            // Supplier type filter
            if (filter.getType() != null && !filter.getType().isBlank()) {
                predicates.add(cb.equal(root.get("supplierTypeCode"), filter.getType()));
            }

            // Scope type filter
            if (filter.getScopeType() != null && !filter.getScopeType().isBlank()) {
                predicates.add(cb.equal(cb.upper(root.get("scopeType").as(String.class)),
                        filter.getScopeType().toUpperCase()));
            }

            // Client filter (scope=CLIENT)
            if (resolvedClientUuid != null) {
                predicates.add(cb.equal(root.get("client").get("id"), resolvedClientUuid));
            }

            // Warehouse/Branch filter (scope=WAREHOUSE). warehouseId maps to branch_id.
            if (resolvedWarehouseUuid != null) {
                predicates.add(cb.equal(root.get("branch").get("id"), resolvedWarehouseUuid));
            }

            // Preferred only filter
            if (Boolean.TRUE.equals(filter.getPreferredOnly())) {
                predicates.add(cb.isTrue(root.get("isPreferred")));
            }

            // Full-text search across multiple fields
            if (filter.getSearch() != null && !filter.getSearch().isBlank()) {
                String pattern = "%" + filter.getSearch().toLowerCase() + "%";
                List<Predicate> searchPredicates = new ArrayList<>();

                searchPredicates.add(cb.like(cb.lower(root.get("legalName")), pattern));
                searchPredicates.add(cb.like(cb.lower(root.get("commercialName")), pattern));
                searchPredicates.add(cb.like(cb.lower(root.get("taxId")), pattern));
                searchPredicates.add(cb.like(cb.lower(root.get("code")), pattern));

                // JOIN to supplier_contacts for email search
                Join<?, ?> contact = root.join("contact", JoinType.LEFT);
                searchPredicates.add(cb.like(cb.lower(contact.get("email")), pattern));

                // JOIN to supplier_addresses for city search
                Join<?, ?> address = root.join("address", JoinType.LEFT);
                searchPredicates.add(cb.like(cb.lower(address.get("city")), pattern));

                predicates.add(cb.or(searchPredicates.toArray(new Predicate[0])));

                // Avoid duplicate results from joins with DISTINCT
                if (query != null) {
                    query.distinct(true);
                }
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
