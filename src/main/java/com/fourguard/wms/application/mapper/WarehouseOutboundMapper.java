package com.fourguard.wms.application.mapper;

import com.fourguard.wms.application.dto.response.outbound.OutboundItemResponse;
import com.fourguard.wms.application.dto.response.outbound.OutboundResponse;
import com.fourguard.wms.application.dto.response.outbound.OutboundSummaryResponse;
import com.fourguard.wms.domain.enums.OutboundStatus;
import com.fourguard.wms.infrastructure.persistence.entity.LocationEntity;
import com.fourguard.wms.infrastructure.persistence.entity.WarehouseOutboundEntity;
import com.fourguard.wms.infrastructure.persistence.entity.WarehouseOutboundItemEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

import java.util.List;

@Mapper(componentModel = "spring")
public interface WarehouseOutboundMapper {

    @Mapping(source = "organization.id", target = "organizationId")
    @Mapping(source = "branch.id", target = "branchId")
    @Mapping(source = "client.id", target = "clientId")
    @Mapping(source = "client.name", target = "clientName")
    @Mapping(target = "destinationId", expression = "java(entity.getDestination() != null ? entity.getDestination().getId() : null)")
    @Mapping(target = "destinationName", expression = "java(entity.getDestinationName() != null && !entity.getDestinationName().isBlank() ? entity.getDestinationName() : (entity.getDestination() != null ? entity.getDestination().getPlantName() : null))")
    @Mapping(target = "destinationAddress", expression = "java(entity.getDestinationAddress() != null && !entity.getDestinationAddress().isBlank() ? entity.getDestinationAddress() : (entity.getDestination() != null ? entity.getDestination().getFullAddress() : null))")
    @Mapping(source = "carrier.id", target = "carrierId")
    @Mapping(source = "carrier.name", target = "carrierName")
    @Mapping(source = "ramp.id", target = "rampId")
    @Mapping(source = "ramp", target = "rampNumber", qualifiedByName = "extractRampNumber")
    @Mapping(source = "ramp.code", target = "rampCode")
    @Mapping(source = "forkliftOperator.id", target = "forkliftOperatorId")
    @Mapping(source = "forkliftOperator.fullName", target = "forkliftOperatorName")
    @Mapping(source = "status", target = "status", qualifiedByName = "outboundStatusToString")
    @Mapping(source = "items", target = "items")
    OutboundResponse toResponse(WarehouseOutboundEntity entity);

    @Mapping(source = "client.id", target = "clientId")
    @Mapping(source = "client.name", target = "clientName")
    @Mapping(target = "destinationId", expression = "java(entity.getDestination() != null ? entity.getDestination().getId() : null)")
    @Mapping(target = "destinationName", expression = "java(entity.getDestinationName() != null && !entity.getDestinationName().isBlank() ? entity.getDestinationName() : (entity.getDestination() != null ? entity.getDestination().getPlantName() : null))")
    @Mapping(target = "destinationAddress", expression = "java(entity.getDestinationAddress() != null && !entity.getDestinationAddress().isBlank() ? entity.getDestinationAddress() : (entity.getDestination() != null ? entity.getDestination().getFullAddress() : null))")
    @Mapping(source = "carrier.id", target = "carrierId")
    @Mapping(source = "carrier.name", target = "carrierName")
    @Mapping(source = "ramp.id", target = "rampId")
    @Mapping(source = "ramp", target = "rampNumber", qualifiedByName = "extractRampNumber")
    @Mapping(source = "ramp.code", target = "rampCode")
    @Mapping(source = "forkliftOperator.id", target = "forkliftOperatorId")
    @Mapping(source = "forkliftOperator.fullName", target = "forkliftOperatorName")
    @Mapping(source = "status", target = "status", qualifiedByName = "outboundStatusToString")
    OutboundSummaryResponse toSummaryResponse(WarehouseOutboundEntity entity);

    @Mapping(source = "item.id", target = "itemId")
    @Mapping(source = "item.sku.code", target = "skuCode")
    @Mapping(source = "item.sku.name", target = "skuDescription")
    @Mapping(source = "item.sapFolio", target = "inboundRemisionNo")
    @Mapping(source = "item.client.name", target = "clientName")
    OutboundItemResponse toItemResponse(WarehouseOutboundItemEntity entity);

    List<OutboundItemResponse> toItemResponseList(List<WarehouseOutboundItemEntity> entities);

    @Named("outboundStatusToString")
    default String outboundStatusToString(OutboundStatus status) {
        return status != null ? status.name() : null;
    }

    @Named("extractRampNumber")
    default Integer extractRampNumber(LocationEntity ramp) {
        if (ramp == null) return null;
        if (ramp.getCode() != null && ramp.getCode().matches(".*\\d+.*")) {
            try {
                String numStr = ramp.getCode().replaceAll("\\D+", "");
                if (!numStr.isBlank()) {
                    return Integer.parseInt(numStr);
                }
            } catch (Exception ignored) {}
        }
        if (ramp.getPosition() != null && ramp.getPosition().matches(".*\\d+.*")) {
            try {
                String numStr = ramp.getPosition().replaceAll("\\D+", "");
                if (!numStr.isBlank()) {
                    return Integer.parseInt(numStr);
                }
            } catch (Exception ignored) {}
        }
        return null;
    }
}
