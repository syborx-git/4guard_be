package com.fourguard.wms.application.dto.response.reception;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Response DTO for a reception seal record.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReceptionSealResponse {

    private UUID id;
    private String sealNumber;
    private OffsetDateTime registeredAt;
}
