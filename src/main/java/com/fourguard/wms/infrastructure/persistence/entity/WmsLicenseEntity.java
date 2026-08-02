package com.fourguard.wms.infrastructure.persistence.entity;

import com.fourguard.wms.domain.enums.LicenseAdminStatus;
import com.fourguard.wms.domain.enums.LicensePlan;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "wms_licenses", schema = "wms")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder(toBuilder = true)
public class WmsLicenseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(updatable = false, nullable = false, columnDefinition = "UUID")
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "organization_id", nullable = false)
    private OrganizationEntity organization;

    @Column(name = "organization_name", nullable = false, length = 150)
    private String organizationName;

    @Column(name = "license_name", nullable = false, length = 150)
    private String licenseName;

    @Column(name = "license_key_hash", nullable = false, length = 255)
    private String licenseKeyHash;

    @Column(name = "masked_license_key", nullable = false, length = 50)
    private String maskedLicenseKey;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private LicensePlan plan;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "valid_from", nullable = false)
    private OffsetDateTime validFrom;

    @Column(name = "valid_until", nullable = false)
    private OffsetDateTime validUntil;

    @Column(name = "grace_period_days")
    @Builder.Default
    private Integer gracePeriodDays = 15;

    @Column(name = "auto_renewal")
    @Builder.Default
    private Boolean autoRenewal = false;

    @Enumerated(EnumType.STRING)
    @Column(name = "admin_status", nullable = false, length = 20)
    @Builder.Default
    private LicenseAdminStatus adminStatus = LicenseAdminStatus.ACTIVE;

    @Column(name = "max_users", nullable = false)
    @Builder.Default
    private Integer maxUsers = 10;

    @Column(name = "max_concurrent_users", nullable = false)
    @Builder.Default
    private Integer maxConcurrentUsers = 5;

    @Column(name = "max_warehouses", nullable = false)
    @Builder.Default
    private Integer maxWarehouses = 1;

    @Column(name = "max_handheld_devices", nullable = false)
    @Builder.Default
    private Integer maxHandheldDevices = 5;

    @Column(name = "max_integrations", nullable = false)
    @Builder.Default
    private Integer maxIntegrations = 1;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "enabled_modules", columnDefinition = "JSONB", nullable = false)
    @Builder.Default
    private List<String> enabledModules = new ArrayList<>(List.of("WMS_CORE"));

    @Column(name = "administrative_reason", columnDefinition = "TEXT")
    private String administrativeReason;

    @Column(columnDefinition = "TEXT")
    private String observations;

    @Column(name = "created_at", updatable = false)
    @Builder.Default
    private OffsetDateTime createdAt = OffsetDateTime.now();

    @Column(name = "updated_at")
    @Builder.Default
    private OffsetDateTime updatedAt = OffsetDateTime.now();

    @Column(name = "updated_by", nullable = false, length = 100)
    @Builder.Default
    private String updatedBy = "SYSTEM";

    @PreUpdate
    public void onPreUpdate() {
        this.updatedAt = OffsetDateTime.now();
    }
}
