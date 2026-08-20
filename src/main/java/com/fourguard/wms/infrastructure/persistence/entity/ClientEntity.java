package com.fourguard.wms.infrastructure.persistence.entity;

import com.fourguard.wms.shared.audit.BaseVersionedEntity;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Entidad JPA — Cliente Depositante / Owner 3PL.
 * Tabla: wms.clients.
 * Gestiona Razón Social, Dirección Fiscal, Matriz de Contactos Corporativos
 * y Múltiples Direcciones Físicas de Destino (Ship-to Locations).
 */
@Entity
@Table(name = "clients", schema = "wms")
@Getter
@Setter
@NoArgsConstructor
@SuperBuilder(toBuilder = true)
public class ClientEntity extends BaseVersionedEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(updatable = false, nullable = false, columnDefinition = "UUID")
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "organization_id", nullable = false)
    private OrganizationEntity organization;

    @Column(nullable = false, length = 200)
    private String name;

    @Column(name = "external_id", length = 50)
    private String externalId;

    @Column(name = "tax_id", length = 30)
    private String taxId;

    /** Dirección Fiscal / Corporativa principal. */
    @Column(length = 300)
    private String address;

    /** Teléfono corporativo principal / conmutador. */
    @Column(length = 50)
    private String phone;

    /** Correo electrónico general de la empresa. */
    @Column(length = 150)
    private String email;

    /** Contraseña de acceso al portal de autoservicio del cliente. */
    @Column(name = "web_portal_password", length = 255)
    private String webPortalPassword;

    @Column(length = 20)
    @Builder.Default
    private String status = "ACTIVE";

    // ── Relación 1:N — Contactos Corporativos ────────────────────────────────
    @OneToMany(
        mappedBy = "client",
        cascade = CascadeType.ALL,
        orphanRemoval = true,
        fetch = FetchType.LAZY
    )
    @Builder.Default
    private List<ClientContactEntity> contacts = new ArrayList<>();

    // ── Relación 1:N — Destinos Físicos / Bodegas ────────────────────────────
    @OneToMany(
        mappedBy = "client",
        cascade = CascadeType.ALL,
        orphanRemoval = true,
        fetch = FetchType.LAZY
    )
    @Builder.Default
    private List<ClientDestinationEntity> destinations = new ArrayList<>();

    // ── Helper methods para gestión de la bidireccionalidad ──────────────────

    public void addContact(ClientContactEntity contact) {
        contacts.add(contact);
        contact.setClient(this);
    }

    public void removeContact(ClientContactEntity contact) {
        contacts.remove(contact);
        contact.setClient(null);
    }

    public void addDestination(ClientDestinationEntity destination) {
        destinations.add(destination);
        destination.setClient(this);
    }

    public void removeDestination(ClientDestinationEntity destination) {
        destinations.remove(destination);
        destination.setClient(null);
    }
}
