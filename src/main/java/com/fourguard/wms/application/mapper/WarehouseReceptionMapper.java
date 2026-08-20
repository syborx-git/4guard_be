package com.fourguard.wms.application.mapper;

import com.fourguard.wms.application.dto.response.reception.ReceptionPalletResponse;
import com.fourguard.wms.application.dto.response.reception.ReceptionResponse;
import com.fourguard.wms.application.dto.response.reception.ReceptionSummaryResponse;
import com.fourguard.wms.domain.enums.PalletType;
import com.fourguard.wms.domain.enums.ReceptionStatus;
import com.fourguard.wms.infrastructure.persistence.entity.WarehouseReceptionEntity;
import com.fourguard.wms.infrastructure.persistence.entity.WarehouseReceptionPalletEntity;
import com.fourguard.wms.infrastructure.persistence.entity.WarehouseReceptionSealEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

import java.util.List;
import java.util.stream.Collectors;

@Mapper(componentModel = "spring")
public interface WarehouseReceptionMapper {

    @Mapping(source = "organization.id", target = "organizationId")
    @Mapping(source = "branch.id", target = "branchId")
    @Mapping(source = "carrier.id", target = "carrierId")
    @Mapping(source = "carrier.name", target = "carrierName")
    @Mapping(source = "client.id", target = "clientId")
    @Mapping(source = "client.name", target = "clientName")
    @Mapping(source = "ramp.id", target = "rampId")
    @Mapping(source = "ramp.name", target = "rampName")
    @Mapping(source = "forkliftOperator.id", target = "forkliftOperatorId")
    @Mapping(source = "forkliftOperator.fullName", target = "forkliftOperatorName")
    @Mapping(source = "sku.id", target = "skuId")
    @Mapping(source = "sku.code", target = "skuCode")
    @Mapping(source = "sku.name", target = "productName")
    @Mapping(source = "supplier.id", target = "supplierId")
    @Mapping(source = "supplier.commercialName", target = "supplierName")
    @Mapping(source = "storageLocation.id", target = "storageLocationId")
    @Mapping(source = "storageLocation.code", target = "storageLocationCode")
    @Mapping(source = "status", target = "status", qualifiedByName = "receptionStatusToString")
    @Mapping(source = "palletType", target = "palletType", qualifiedByName = "palletTypeToString")
    @Mapping(source = "palletType", target = "palletTypeLabel", qualifiedByName = "palletTypeToLabel")
    @Mapping(source = "seals", target = "sealNumbers", qualifiedByName = "mapSealsToStrings")
    @Mapping(source = "pallets", target = "pallets")
    @Mapping(target = "totalPallets", expression = "java(entity.getPallets() != null ? entity.getPallets().size() : 0)")
    @Mapping(target = "totalPieces", expression = "java(entity.getPallets() != null ? entity.getPallets().stream().mapToDouble(p -> p.getPieces() != null ? p.getPieces().doubleValue() : 0.0).sum() : 0.0)")
    ReceptionResponse toResponse(WarehouseReceptionEntity entity);

    @Mapping(source = "client.id", target = "clientId")
    @Mapping(source = "client.name", target = "clientName")
    @Mapping(source = "carrier.id", target = "carrierId")
    @Mapping(source = "carrier.name", target = "carrierName")
    @Mapping(source = "ramp.id", target = "rampId")
    @Mapping(source = "ramp.name", target = "rampName")
    @Mapping(source = "sku.id", target = "skuId")
    @Mapping(source = "sku.code", target = "skuCode")
    @Mapping(source = "sku.name", target = "productName")
    @Mapping(source = "status", target = "status", qualifiedByName = "receptionStatusToString")
    @Mapping(source = "createdBy", target = "capturedBy")
    @Mapping(target = "totalPallets", expression = "java(entity.getPallets() != null ? entity.getPallets().size() : 0)")
    @Mapping(target = "totalPieces", expression = "java(entity.getPallets() != null ? entity.getPallets().stream().mapToDouble(p -> p.getPieces() != null ? p.getPieces().doubleValue() : 0.0).sum() : 0.0)")
    ReceptionSummaryResponse toSummaryResponse(WarehouseReceptionEntity entity);

    @Mapping(source = "sku.id", target = "skuId")
    @Mapping(source = "sku.code", target = "skuCode")
    @Mapping(source = "sku.name", target = "description")
    @Mapping(source = "supplier.id", target = "supplierId")
    @Mapping(source = "supplier.commercialName", target = "supplierName")
    @Mapping(source = "palletType", target = "palletTypeId", qualifiedByName = "palletTypeToString")
    @Mapping(source = "palletType", target = "palletTypeLabel", qualifiedByName = "palletTypeToLabel")
    @Mapping(source = "inventoryItem.id", target = "inventoryItemId")
    ReceptionPalletResponse toPalletResponse(WarehouseReceptionPalletEntity entity);

    List<ReceptionPalletResponse> toPalletResponseList(List<WarehouseReceptionPalletEntity> entities);

    @Named("receptionStatusToString")
    default String receptionStatusToString(ReceptionStatus status) {
        return status != null ? status.name() : null;
    }

    @Named("palletTypeToString")
    default String palletTypeToString(PalletType type) {
        return type != null ? type.name() : null;
    }

    @Named("palletTypeToLabel")
    default String palletTypeToLabel(PalletType type) {
        if (type == null) return null;
        return switch (type) {
            case MADERA_ESTANDAR -> "Madera Estándar";
            case TARIMA_CHEP -> "Tarima CHEP";
            case PLASTICO -> "Plástico";
            case PLASTICO_AZUL -> "Plástico Azul";
            case MADERA_EXPORTACION -> "Madera Exportación";
            case SIN_TARIMA -> "Sin Tarima";
            case MADERA -> "Madera";
        };
    }

    @Named("mapSealsToStrings")
    default List<String> mapSealsToStrings(List<WarehouseReceptionSealEntity> seals) {
        if (seals == null) return List.of();
        return seals.stream().map(WarehouseReceptionSealEntity::getSealNumber).collect(Collectors.toList());
    }
}
