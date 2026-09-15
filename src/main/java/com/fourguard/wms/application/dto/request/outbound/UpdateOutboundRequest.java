package com.fourguard.wms.application.dto.request.outbound;

import lombok.Data;

import java.util.List;
import java.util.UUID;

/**
 * Request DTO for updating an Outbound dispatch (Lifecycle progression or Caseta modification).
 */
@Data
public class UpdateOutboundRequest {

    private String status;
    private UUID destinationId;
    private String destinationName;
    private String destinationAddress;

    private UUID carrierId;
    private String carrierName;

    private UUID rampId;
    private Integer rampNumber;

    private UUID forkliftOperatorId;
    private String forkliftOperatorName;

    private String transportType;
    private String driverName;
    private String economicNumber;
    private String boxEconomicNumber;
    private String tractorPlates;
    private String boxPlates;

    private String sealNumber;
    private String remisionNo;
    private String observations;

    private List<UUID> selectedItemIds;
}
