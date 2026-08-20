package com.fourguard.wms.infrastructure.persistence.adapter;

import com.fourguard.wms.application.dto.request.AlertConfigFilterRequest;
import com.fourguard.wms.domain.ports.out.AlertConfigRepositoryPort;
import com.fourguard.wms.infrastructure.persistence.entity.AlertConfigEntity;
import com.fourguard.wms.infrastructure.persistence.repository.AlertConfigJpaRepository;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class AlertConfigPersistenceAdapter implements AlertConfigRepositoryPort {

    private final AlertConfigJpaRepository repository;

    @Override
    public AlertConfigEntity save(AlertConfigEntity entity) {
        return repository.save(entity);
    }

    @Override
    public Optional<AlertConfigEntity> findById(UUID id) {
        return repository.findByIdAndIsDeletedFalse(id);
    }

    @Override
    public List<AlertConfigEntity> findAll(AlertConfigFilterRequest filter) {
        Specification<AlertConfigEntity> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            // Always filter out soft-deleted items
            predicates.add(cb.equal(root.get("isDeleted"), false));

            if (filter != null) {
                if (filter.getOrganizationId() != null) {
                    predicates.add(cb.equal(root.get("organization").get("id"), filter.getOrganizationId()));
                }
                if (filter.getCategory() != null) {
                    predicates.add(cb.equal(root.get("category"), filter.getCategory()));
                }
                if (filter.getEvent() != null) {
                    predicates.add(cb.equal(root.get("event"), filter.getEvent()));
                }
                if (filter.getPriority() != null) {
                    predicates.add(cb.equal(root.get("priority"), filter.getPriority()));
                }
                if (filter.getStatus() != null) {
                    predicates.add(cb.equal(root.get("status"), filter.getStatus()));
                }
                if (filter.getSearch() != null && !filter.getSearch().isBlank()) {
                    String searchPattern = "%" + filter.getSearch().trim().toLowerCase() + "%";
                    Predicate nameMatch = cb.like(cb.lower(root.get("name")), searchPattern);
                    Predicate descMatch = cb.like(cb.lower(root.get("description")), searchPattern);
                    predicates.add(cb.or(nameMatch, descMatch));
                }
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };

        return repository.findAll(spec);
    }

    @Override
    public boolean existsByOrganizationIdAndNameAndIsDeletedFalse(UUID organizationId, String name) {
        return repository.existsByOrganizationIdAndNameAndIsDeletedFalse(organizationId, name);
    }

    @Override
    public boolean existsByOrganizationIdAndNameAndIdNotAndIsDeletedFalse(UUID organizationId, String name, UUID id) {
        return repository.existsByOrganizationIdAndNameAndIdNotAndIsDeletedFalse(organizationId, name, id);
    }

    @Override
    public void softDelete(UUID id) {
        repository.findById(id).ifPresent(entity -> {
            entity.setIsDeleted(true);
            entity.setStatus(com.fourguard.wms.domain.enums.AlertStatus.INACTIVE);
            entity.setDeletedAt(java.time.OffsetDateTime.now(java.time.ZoneOffset.UTC));
            repository.save(entity);
        });
    }
}
