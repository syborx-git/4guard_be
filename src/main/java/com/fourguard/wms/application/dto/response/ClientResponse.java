package com.fourguard.wms.application.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.time.OffsetDateTime;
import java.util.UUID;

/** Response DTO for Client. */
@Getter
@Builder
public class ClientResponse {
    private final UUID id;
    private final UUID organizationId;
    private final String organizationName;
    private final String name;
    private final String externalId;
    private final String taxId;
    private final String status;
    private final Long version;
    private final OffsetDateTime createdAt;
    private final OffsetDateTime updatedAt;
}
