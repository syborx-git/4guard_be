package com.fourguard.wms.infrastructure.persistence.repository.chat;

import com.fourguard.wms.infrastructure.persistence.entity.chat.ChatSecurityEventEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.UUID;

@Repository
public interface ChatSecurityEventJpaRepository extends JpaRepository<ChatSecurityEventEntity, UUID> {

    long countByOrganizationIdAndCreatedAtAfter(UUID organizationId, Instant since);

    Page<ChatSecurityEventEntity> findByOrganizationIdAndCreatedAtAfterOrderByCreatedAtDesc(
            UUID organizationId,
            Instant since,
            Pageable pageable
    );
}
