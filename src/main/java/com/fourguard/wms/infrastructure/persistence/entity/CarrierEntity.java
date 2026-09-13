package com.fourguard.wms.infrastructure.persistence.entity;

import com.fourguard.wms.domain.enums.CarrierStatus;
import com.fourguard.wms.domain.enums.CarrierType;
import com.fourguard.wms.domain.enums.ServiceType;
import com.fourguard.wms.shared.audit.BaseVersionedEntity;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "carriers", schema = "wms",
        uniqueConstraints = @UniqueConstraint(name = "uk_carrier_org_name", columnNames = {"organization_id", "name"}))
@Getter
@Setter
@NoArgsConstructor
@SuperBuilder(toBuilder = true)
public class CarrierEntity extends BaseVersionedEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(updatable = false, nullable = false, columnDefinition = "UUID")
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "organization_id", nullable = false)
    private OrganizationEntity organization;

    @Column(nullable = false, length = 200)
    private String name;

    @Column(name = "trade_name", nullable = false, length = 200)
    private String tradeName;

    @Column(name = "tax_id", length = 30)
    private String taxId;

    @Enumerated(EnumType.STRING)
    @Column(name = "carrier_type", nullable = false, length = 50)
    @Builder.Default
    private CarrierType carrierType = CarrierType.EXTERNAL;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private CarrierStatus status = CarrierStatus.ACTIVE;

    @Column(name = "contact_name", length = 150)
    private String contactName;

    @Column(name = "contact_phone", length = 20)
    private String contactPhone;

    @Column(name = "contact_email", length = 255)
    private String contactEmail;

    @Enumerated(EnumType.STRING)
    @Column(name = "service_type", nullable = false, length = 50)
    @Builder.Default
    private ServiceType serviceType = ServiceType.FTL;

    @Column(name = "permit_number", length = 100)
    private String permitNumber;

    @Column(name = "geographic_coverage", columnDefinition = "TEXT")
    private String geographicCoverage;

    @Column(columnDefinition = "TEXT")
    private String notes;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(
            name = "carrier_vehicle_types",
            schema = "wms",
            joinColumns = @JoinColumn(name = "carrier_id")
    )
    @Column(name = "vehicle_type")
    @Builder.Default
    private List<String> vehicleTypes = new ArrayList<>();

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "carrier_preferred_clients",
            schema = "wms",
            joinColumns = @JoinColumn(name = "carrier_id"),
            inverseJoinColumns = @JoinColumn(name = "client_id")
    )
    @Builder.Default
    private List<ClientEntity> preferredClients = new ArrayList<>();
}
