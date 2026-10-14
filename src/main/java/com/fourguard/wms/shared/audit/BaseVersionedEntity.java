package com.fourguard.wms.shared.audit;

import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.Version;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.DynamicUpdate;

/**
 * Extends {@link BaseAuditEntity} adding optimistic locking ({@code @Version}).
 *
 * <p>{@code @SuperBuilder} propagates the builder chain through the hierarchy so that
 * all entity subclasses can use their generated builders with all inherited fields visible.</p>
 */
@SuperBuilder(toBuilder = true)
@NoArgsConstructor
@Getter
@Setter
@MappedSuperclass
@DynamicUpdate
public abstract class BaseVersionedEntity extends BaseAuditEntity {

    protected BaseVersionedEntity(BaseVersionedEntityBuilder<?, ?> b) {
        super(b);
        this.version = b.version;
    }

    @Version
    @Column(name = "version")
    private Long version;
}
