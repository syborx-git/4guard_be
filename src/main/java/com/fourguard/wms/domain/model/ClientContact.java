package com.fourguard.wms.domain.model;

import lombok.*;

import java.time.OffsetDateTime;
import java.util.UUID;

/** Modelo de dominio — Contacto Corporativo de un Cliente 3PL. */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder(toBuilder = true)
public class ClientContact {
    private UUID    id;
    private UUID    clientId;
    private String  name;
    private String  department;
    private String  phone;
    private String  email;
    private Boolean isPrimary;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
}
