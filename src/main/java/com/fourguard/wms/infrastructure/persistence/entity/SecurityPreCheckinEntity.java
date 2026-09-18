package com.fourguard.wms.infrastructure.persistence.entity;

import com.fourguard.wms.shared.audit.BaseVersionedEntity;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * JPA Entity for wms.security_pre_checkins.
 * Stores digital QR access passes and driver self-registration records (F01-PO-CP-7.1.3-03).
 */
@Entity
@Table(
    name = "security_pre_checkins",
    schema = "wms",
    uniqueConstraints = {
        @UniqueConstraint(name = "uk_security_precheckins_token", columnNames = {"token"})
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder(toBuilder = true)
public class SecurityPreCheckinEntity extends BaseVersionedEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(updatable = false, nullable = false, columnDefinition = "UUID")
    private UUID id;

    @Column(nullable = false, length = 64, unique = true)
    private String token;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "organization_id", nullable = false)
    private OrganizationEntity organization;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "branch_id", nullable = false)
    private BranchEntity branch;

    @Column(nullable = false, length = 30)
    @Builder.Default
    private String status = "PENDING_DRIVER"; // PENDING_DRIVER, SUBMITTED, COMPLETED, CANCELLED

    @Column(name = "operation_type", nullable = false, length = 20)
    @Builder.Default
    private String operationType = "DESCARGA"; // CARGA, DESCARGA

    // ── Client & Carrier ──
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "client_id")
    private ClientEntity client;

    @Column(name = "client_code", length = 100)
    private String clientCode;

    @Column(name = "client_name", length = 200)
    private String clientName;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "carrier_id")
    private CarrierEntity carrier;

    @Column(name = "carrier_line_code", length = 100)
    private String carrierLineCode;

    @Column(name = "carrier_line", length = 200)
    private String carrierLine;

    // ── Transport & Driver ──
    @Column(name = "driver_name", length = 200)
    private String driverName;

    @Column(name = "driver_license", length = 100)
    private String driverLicense;

    @Column(name = "transport_type", length = 100)
    private String transportType;

    @Column(name = "economic_number", length = 100)
    private String economicNumber;

    @Column(name = "no_eco_tractor", length = 100)
    private String noEcoTractor;

    @Column(name = "box_economic_number", length = 100)
    private String boxEconomicNumber;

    @Column(name = "tractor_plates", length = 50)
    private String tractorPlates;

    @Column(name = "box_plates", length = 50)
    private String boxPlates;

    @Column(name = "box_dimensions", length = 50)
    private String boxDimensions;

    @JdbcTypeCode(SqlTypes.ARRAY)
    @Column(name = "seal_numbers", columnDefinition = "TEXT[]")
    @Builder.Default
    private List<String> sealNumbers = new ArrayList<>();

    // ── Documents ──
    @Column(name = "doc_number", length = 100)
    private String docNumber;

    @Column(name = "doc_date")
    private LocalDate docDate;

    @Column(name = "reception_time")
    private LocalTime receptionTime;

    @Column(name = "departure_time")
    private LocalTime departureTime;

    // ── Checklist & Inspection (F01-PO-CP-7.1.3-03) ──
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "checklist_data", columnDefinition = "JSONB")
    private String checklistData;

    @Column(name = "observations", columnDefinition = "TEXT")
    private String observations;

    @Column(name = "driver_signature", columnDefinition = "TEXT")
    private String driverSignature;

    @Column(name = "driver_signed_at")
    private OffsetDateTime driverSignedAt;

    // ── Resolution by Security Guard ──
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ramp_id")
    private LocationEntity ramp;

    @Column(name = "ramp_number")
    private Integer rampNumber;

    @Column(name = "ramp_code", length = 50)
    private String rampCode;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "forklift_operator_id")
    private ForkliftOperatorEntity forkliftOperator;

    @Column(name = "forklift_operator_name", length = 150)
    private String forkliftOperatorName;

    @Column(name = "guard_notes", columnDefinition = "TEXT")
    private String guardNotes;

    @Column(name = "generated_folio", length = 50)
    private String generatedFolio;

    @Column(name = "processed_by", length = 100)
    private String processedBy;

    @Column(name = "processed_at")
    private OffsetDateTime processedAt;

    // ── Check-Out & Departure (Salida de Planta) ──
    @Column(name = "exit_observations", columnDefinition = "TEXT")
    private String exitObservations;

    @JdbcTypeCode(SqlTypes.ARRAY)
    @Column(name = "exit_seal_numbers", columnDefinition = "TEXT[]")
    @Builder.Default
    private List<String> exitSealNumbers = new ArrayList<>();

    @Column(name = "exited_by", length = 100)
    private String exitedBy;

    @Column(name = "exited_at")
    private OffsetDateTime exitedAt;

    @Column(name = "expires_at", nullable = false)
    private OffsetDateTime expiresAt;
}
