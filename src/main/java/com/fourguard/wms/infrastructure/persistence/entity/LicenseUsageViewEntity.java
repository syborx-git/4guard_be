package com.fourguard.wms.infrastructure.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Immutable;

import java.util.UUID;

@Entity
@Immutable
@Table(name = "v_license_usage", schema = "wms")
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LicenseUsageViewEntity {

    @Id
    @Column(name = "license_id")
    private UUID licenseId;

    @Column(name = "current_users")
    private Long currentUsers;

    @Column(name = "concurrent_users_peak")
    private Long concurrentUsersPeak;

    @Column(name = "current_warehouses")
    private Long currentWarehouses;

    @Column(name = "registered_handheld_devices")
    private Long registeredHandheldDevices;

    @Column(name = "active_integrations")
    private Long activeIntegrations;
}
