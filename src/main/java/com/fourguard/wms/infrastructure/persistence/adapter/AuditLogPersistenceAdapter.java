package com.fourguard.wms.infrastructure.persistence.adapter;

import com.fourguard.wms.domain.ports.out.AuditLogRepositoryPort;
import com.fourguard.wms.infrastructure.persistence.entity.AuditLogEntity;
import com.fourguard.wms.infrastructure.persistence.repository.AuditLogJpaRepository;
import lombok.RequiredArgsConstructor;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Component;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class AuditLogPersistenceAdapter implements AuditLogRepositoryPort {

    private final AuditLogJpaRepository repository;

    @Override
    public AuditLogEntity log(AuditLogEntity entry) {
        // Only INSERT — no update or delete (WORM enforced by DB trigger)
        return repository.save(entry);
    }

    @Override
    public List<AuditLogEntity> findByEntityTypeAndEntityId(String type, UUID id) {
        return repository.findByEntityTypeAndEntityId(type, id);
    }

    @Override
    public List<AuditLogEntity> findByUserId(UUID userId) {
        return repository.findByUserId(userId);
    }

    @Override
    public List<AuditLogEntity> findByActionAndCreatedAtAfter(String action, OffsetDateTime since) {
        return repository.findByActionAndCreatedAtAfter(action, since);
    }

    @Override
    public Optional<AuditLogEntity> findLastLogoutForUserAfter(UUID userId, OffsetDateTime timestamp) {
        return repository.findLastLogoutForUserAfter(userId, timestamp);
    }

    @Override
    public List<AuditLogEntity> findUserActivity(UUID userId, String action, OffsetDateTime fromDate, OffsetDateTime toDate) {
        Specification<AuditLogEntity> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (userId != null) {
                predicates.add(cb.equal(root.get("userId"), userId));
            }
            if (action != null && !action.isBlank()) {
                predicates.add(cb.equal(cb.lower(root.get("action")), action.trim().toLowerCase()));
            }
            if (fromDate != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("createdAt"), fromDate));
            }
            if (toDate != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("createdAt"), toDate));
            }

            if (query != null) {
                query.orderBy(cb.desc(root.get("createdAt")));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };

        return repository.findAll(spec);
    }
}


