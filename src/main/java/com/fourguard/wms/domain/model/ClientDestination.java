package com.fourguard.wms.domain.model;

import lombok.*;

import java.time.OffsetDateTime;
import java.util.UUID;

/** Modelo de dominio — Dirección Física de Destino (Bodega / Planta / Ship-to). */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder(toBuilder = true)
public class ClientDestination {
    private UUID   id;
    private UUID   clientId;
    private String destinationCode;  // Código correlativo ej. DEST-TOL-01
    private String plantName;        // Nombre de la planta o bodega
    private String fullAddress;      // Dirección física completa de entrega
    private String contactPerson;    // Responsable en sitio
    private String phone;            // Teléfono directo de la planta
    private String status;           // ACTIVO | INACTIVO
    private String notes;            // Indicaciones especiales de acceso/descarga
    private Long   version;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
}
