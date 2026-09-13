package com.fourguard.wms.infrastructure.persistence.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.DynamicUpdate;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.UUID;

/**
 * JPA entity for wms.supplier_contacts.
 * 1:1 with SupplierEntity (UNIQUE constraint on supplier_id).
 * Cascade from SupplierEntity — never persisted independently.
 */
@Entity
@Table(name = "supplier_contacts", schema = "wms")
@DynamicUpdate
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SupplierContactEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(updatable = false, nullable = false, columnDefinition = "UUID")
    private UUID id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "supplier_id", nullable = false, unique = true)
    private SupplierEntity supplier;

    @Column(name = "full_name", length = 150, nullable = false)
    private String fullName;

    @Column(name = "job_title", length = 100)
    private String jobTitle;

    @Column(name = "email", length = 150, nullable = false)
    private String email;

    @Column(name = "phone", length = 25)
    private String phone;

    @Column(name = "alt_phone", length = 25)
    private String altPhone;

    @Column(name = "created_at", updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at")
    private OffsetDateTime updatedAt;

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
