package com.fourguard.wms.infrastructure.persistence.entity;

import com.fourguard.wms.shared.audit.BaseVersionedEntity;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.util.UUID;

/**
 * Entidad JPA — Dirección Física de Destino (Bodega / Planta / Ship-to Location).
 * Tabla: wms.client_destinations.
 */
@Entity
@Table(
    name = "client_destinations",
    schema = "wms",
    uniqueConstraints = @UniqueConstraint(
        name = "uk_client_destination_code",
        columnNames = {"client_id", "destination_code"}
    )
)
@Getter
@Setter
@NoArgsConstructor
@SuperBuilder(toBuilder = true)
public class ClientDestinationEntity extends BaseVersionedEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(updatable = false, nullable = false, columnDefinition = "UUID")
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "client_id", nullable = false)
    private ClientEntity client;

    @Column(name = "destination_code", nullable = false, length = 50)
    private String destinationCode;

    @Column(name = "plant_name", nullable = false, length = 200)
    private String plantName;

    @Column(name = "full_address", nullable = false, length = 500)
    private String fullAddress;

    @Column(name = "contact_person", nullable = false, length = 150)
    private String contactPerson;

    @Column(nullable = false, length = 50)
    private String phone;

    @Column(length = 20)
    @Builder.Default
    private String status = "ACTIVO";

    @Column(columnDefinition = "TEXT")
    private String notes;
}
