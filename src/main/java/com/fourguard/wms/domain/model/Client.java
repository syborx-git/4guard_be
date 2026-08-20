package com.fourguard.wms.domain.model;

import lombok.*;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/** Modelo de dominio — Cliente Depositante / Owner 3PL. */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder(toBuilder = true)
public class Client {
    private UUID   id;
    private Organization organization;
    private String name;           // Razón Social
    private String externalId;     // RFC / Código ERP
    private String taxId;          // Tax ID / RFC SAT
    private String address;        // Dirección Fiscal / Corporativa principal
    private String phone;          // Teléfono corporativo principal
    private String email;          // Correo general
    private String webPortalPassword; // Contraseña Portal Autoservicio
    private String status;         // ACTIVE | INACTIVE
    private Long   version;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
    private String createdBy;
    private String updatedBy;

    @Builder.Default
    private List<ClientContact>     contacts     = new ArrayList<>();
    @Builder.Default
    private List<ClientDestination> destinations = new ArrayList<>();
}

