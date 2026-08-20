package com.fourguard.wms.application.mapper;

import com.fourguard.wms.application.dto.response.outbound.OutboundItemResponse;
import com.fourguard.wms.application.dto.response.outbound.OutboundResponse;
import com.fourguard.wms.application.dto.response.outbound.OutboundSummaryResponse;
import com.fourguard.wms.domain.enums.OutboundStatus;
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
    @Mapping(source = "destination.id", target = "destinationId")
    @Mapping(source = "carrier.id", target = "carrierId")
    @Mapping(source = "carrier.name", target = "carrierName")
    @Mapping(source = "status", target = "status", qualifiedByName = "outboundStatusToString")
    @Mapping(source = "items", target = "items")
    OutboundResponse toResponse(WarehouseOutboundEntity entity);

    @Mapping(source = "client.id", target = "clientId")
    @Mapping(source = "client.name", target = "clientName")
    @Mapping(source = "destination.id", target = "destinationId")
    @Mapping(source = "carrier.id", target = "carrierId")
    @Mapping(source = "carrier.name", target = "carrierName")
    @Mapping(source = "status", target = "status", qualifiedByName = "outboundStatusToString")
    OutboundSummaryResponse toSummaryResponse(WarehouseOutboundEntity entity);

    @Mapping(source = "item.id", target = "itemId")
    @Mapping(source = "item.sku.code", target = "skuCode")
    @Mapping(source = "item.sku.name", target = "skuDescription")
    OutboundItemResponse toItemResponse(WarehouseOutboundItemEntity entity);

    List<OutboundItemResponse> toItemResponseList(List<WarehouseOutboundItemEntity> entities);

    @Named("outboundStatusToString")
    default String outboundStatusToString(OutboundStatus status) {
        return status != null ? status.name() : null;
    }
}
