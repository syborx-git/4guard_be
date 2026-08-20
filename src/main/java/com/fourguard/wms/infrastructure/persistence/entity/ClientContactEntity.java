package com.fourguard.wms.infrastructure.persistence.entity;

import com.fourguard.wms.shared.audit.BaseAuditEntity;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.util.UUID;

/** Entidad JPA — Contacto Corporativo de un Cliente 3PL. Tabla: wms.client_contacts. */
@Entity
@Table(name = "client_contacts", schema = "wms")
@Getter
@Setter
@NoArgsConstructor
@SuperBuilder(toBuilder = true)
public class ClientContactEntity extends BaseAuditEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(updatable = false, nullable = false, columnDefinition = "UUID")
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "client_id", nullable = false)
    private ClientEntity client;

    @Column(nullable = false, length = 150)
    private String name;

    @Column(nullable = false, length = 100)
    private String department;

    @Column(nullable = false, length = 50)
    private String phone;

    @Column(nullable = false, length = 150)
    private String email;

    @Column(name = "is_primary")
    @Builder.Default
    private Boolean isPrimary = false;
}
