package com.fourguard.wms.application.mapper;

import com.fourguard.wms.application.dto.response.transfer.TransferItemResponse;
import com.fourguard.wms.application.dto.response.transfer.TransferResponse;
import com.fourguard.wms.application.dto.response.transfer.TransferSummaryResponse;
import com.fourguard.wms.domain.enums.TransferReason;
import com.fourguard.wms.domain.enums.TransferStatus;
import com.fourguard.wms.infrastructure.persistence.entity.WarehouseTransferEntity;
import com.fourguard.wms.infrastructure.persistence.entity.WarehouseTransferItemEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

import java.util.List;

@Mapper(componentModel = "spring")
public interface WarehouseTransferMapper {

    @Mapping(source = "organization.id", target = "organizationId")
    @Mapping(source = "branch.id", target = "branchId")
    @Mapping(source = "originLocation.id", target = "originLocationId")
    @Mapping(source = "originLocation.code", target = "originLocationCode")
    @Mapping(source = "destinationLocation.id", target = "destinationLocationId")
    @Mapping(source = "destinationLocation.code", target = "destinationLocationCode")
    @Mapping(source = "forkliftOperator.id", target = "forkliftOperatorId")
    @Mapping(source = "forkliftOperator.fullName", target = "forkliftOperatorName")
    @Mapping(source = "status", target = "status", qualifiedByName = "transferStatusToString")
    @Mapping(source = "reasonCode", target = "reasonCode", qualifiedByName = "transferReasonToString")
    @Mapping(source = "items", target = "items")
    TransferResponse toResponse(WarehouseTransferEntity entity);

    @Mapping(source = "originLocation.id", target = "originLocationId")
    @Mapping(source = "originLocation.code", target = "originLocationCode")
    @Mapping(source = "destinationLocation.id", target = "destinationLocationId")
    @Mapping(source = "destinationLocation.code", target = "destinationLocationCode")
    @Mapping(source = "forkliftOperator.id", target = "forkliftOperatorId")
    @Mapping(source = "forkliftOperator.fullName", target = "forkliftOperatorName")
    @Mapping(source = "status", target = "status", qualifiedByName = "transferStatusToString")
    @Mapping(source = "reasonCode", target = "reasonCode", qualifiedByName = "transferReasonToString")
    TransferSummaryResponse toSummaryResponse(WarehouseTransferEntity entity);

    @Mapping(source = "item.id", target = "itemId")
    @Mapping(source = "item.sku.code", target = "skuCode")
    @Mapping(source = "item.sku.name", target = "skuDescription")
    TransferItemResponse toItemResponse(WarehouseTransferItemEntity entity);

    List<TransferItemResponse> toItemResponseList(List<WarehouseTransferItemEntity> entities);

    @Named("transferStatusToString")
    default String transferStatusToString(TransferStatus status) {
        return status != null ? status.name() : null;
    }

    @Named("transferReasonToString")
    default String transferReasonToString(TransferReason reason) {
        return reason != null ? reason.name() : null;
    }
}
