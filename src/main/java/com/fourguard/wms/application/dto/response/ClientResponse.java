package com.fourguard.wms.application.dto.response;

import com.fourguard.wms.application.dto.common.ClientContactDto;
import com.fourguard.wms.application.dto.common.PhysicalDestinationDto;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/** Response DTO — Cliente Depositante / Owner 3PL. */
@Getter
@Builder
@Schema(description = "Información completa de un cliente depositante 3PL")
public class ClientResponse {
    private final UUID   id;
    private final UUID   organizationId;
    private final String organizationName;
    private final String name;
    private final String externalId;
    private final String taxId;
    private final String address;
    private final String phone;
    private final String email;
    private final String webPortalPassword;
    private final String status;

    @Builder.Default
    private final List<ClientContactDto>     contacts     = new ArrayList<>();

    @Builder.Default
    private final List<PhysicalDestinationDto> destinations = new ArrayList<>();

    private final Long            version;
    private final OffsetDateTime  createdAt;
    private final OffsetDateTime  updatedAt;
    private final String          createdBy;
    private final String          updatedBy;
}
