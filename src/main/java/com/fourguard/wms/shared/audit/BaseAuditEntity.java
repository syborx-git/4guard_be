package com.fourguard.wms.shared.audit;

import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;

/**
 * JPA MappedSuperclass with audit fields and lifecycle callbacks.
 * Used exclusively by infrastructure/persistence/entity classes — never by domain models.
 *
 * <p>{@code @SuperBuilder} enables Lombok builder inheritance so subclasses can include
 * these fields in their generated builders, which MapStruct requires to map {@code ignore=true}
 * annotations targeting inherited properties.</p>
 */
@SuperBuilder(toBuilder = true)
@NoArgsConstructor
@Getter
@Setter
@MappedSuperclass
public abstract class BaseAuditEntity {

    protected BaseAuditEntity(BaseAuditEntityBuilder<?, ?> b) {
        this.createdAt = b.createdAt;
        this.updatedAt = b.updatedAt;
        this.createdBy = b.createdBy;
        this.updatedBy = b.updatedBy;
    }

    @Column(name = "created_at", updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at")
    private OffsetDateTime updatedAt;

    @Column(name = "created_by", length = 36, updatable = false)
    private String createdBy;

    @Column(name = "updated_by", length = 36)
    private String updatedBy;

    @PrePersist
    protected void onPrePersist() {
        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        if (createdAt == null) createdAt = now;
        if (updatedAt == null) updatedAt = now;
    }

    @PreUpdate
    protected void onPreUpdate() {
        updatedAt = OffsetDateTime.now(ZoneOffset.UTC);
    }
}
