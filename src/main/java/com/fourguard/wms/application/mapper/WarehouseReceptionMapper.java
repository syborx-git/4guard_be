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
import org.mapstruct.ReportingPolicy;

import java.util.List;
import java.util.stream.Collectors;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface WarehouseReceptionMapper {

    @Mapping(source = "organization.id", target = "organizationId")
    @Mapping(source = "branch.id", target = "branchId")
    @Mapping(source = "carrier.id", target = "carrierId")
    @Mapping(source = "carrier.name", target = "carrierName")
    @Mapping(source = "client.id", target = "clientId")
    @Mapping(source = "client.name", target = "clientName")
    @Mapping(source = "ramp.id", target = "rampId")
    @Mapping(source = "ramp.name", target = "rampName")
    @Mapping(source = "ramp.code", target = "rampCode")
    @Mapping(source = "forkliftOperator.id", target = "forkliftOperatorId")
    @Mapping(source = "forkliftOperator", target = "forkliftOperatorName", qualifiedByName = "forkliftOperatorToName")
    @Mapping(source = "forkliftOperator.code", target = "forkliftOperatorCode")
    @Mapping(source = "sku.id", target = "skuId")
    @Mapping(source = "sku.code", target = "skuCode")
    @Mapping(source = "sku.name", target = "productName")
    @Mapping(source = "supplier.id", target = "supplierId")
    @Mapping(source = "supplier", target = "supplierName", qualifiedByName = "supplierToName")
    @Mapping(source = "storageLocation.id", target = "storageLocationId")
    @Mapping(source = "storageLocation.code", target = "storageLocationCode")
    @Mapping(source = "status", target = "status", qualifiedByName = "receptionStatusToString")
    @Mapping(source = "palletType", target = "palletType", qualifiedByName = "palletTypeToString")
    @Mapping(source = "palletType", target = "palletTypeLabel", qualifiedByName = "palletTypeToLabel")
    @Mapping(source = "seals", target = "sealNumbers", qualifiedByName = "mapSealsToStrings")
    @Mapping(target = "economicNumber", expression = "java(entity.getPreCheckin() != null ? (entity.getPreCheckin().getEconomicNumber() != null && !entity.getPreCheckin().getEconomicNumber().isBlank() ? entity.getPreCheckin().getEconomicNumber() : entity.getPreCheckin().getNoEcoTractor()) : null)")
    @Mapping(target = "noEcoTractor", expression = "java(entity.getPreCheckin() != null ? (entity.getPreCheckin().getNoEcoTractor() != null && !entity.getPreCheckin().getNoEcoTractor().isBlank() ? entity.getPreCheckin().getNoEcoTractor() : entity.getPreCheckin().getEconomicNumber()) : null)")
    @Mapping(target = "boxEconomicNumber", expression = "java(entity.getPreCheckin() != null ? (entity.getPreCheckin().getBoxEconomicNumber() != null && !entity.getPreCheckin().getBoxEconomicNumber().isBlank() ? entity.getPreCheckin().getBoxEconomicNumber() : null) : null)")
    @Mapping(target = "noEcoCaja", expression = "java(entity.getPreCheckin() != null ? (entity.getPreCheckin().getBoxEconomicNumber() != null && !entity.getPreCheckin().getBoxEconomicNumber().isBlank() ? entity.getPreCheckin().getBoxEconomicNumber() : null) : null)")
    @Mapping(source = "lots", target = "lots")
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
    @Mapping(source = "ramp.code", target = "rampCode")
    @Mapping(source = "forkliftOperator.id", target = "forkliftOperatorId")
    @Mapping(source = "forkliftOperator", target = "forkliftOperatorName", qualifiedByName = "forkliftOperatorToName")
    @Mapping(source = "forkliftOperator.code", target = "forkliftOperatorCode")
    @Mapping(source = "sku.id", target = "skuId")
    @Mapping(source = "sku.code", target = "skuCode")
    @Mapping(source = "sku.name", target = "productName")
    @Mapping(source = "supplier.id", target = "supplierId")
    @Mapping(source = "supplier", target = "supplierName", qualifiedByName = "supplierToName")
    @Mapping(source = "status", target = "status", qualifiedByName = "receptionStatusToString")
    @Mapping(source = "storageLocation.id", target = "storageLocationId")
    @Mapping(source = "storageLocation.code", target = "storageLocationCode")
    @Mapping(source = "observations", target = "observations")
    @Mapping(source = "createdBy", target = "capturedBy")
    @Mapping(target = "economicNumber", expression = "java(entity.getPreCheckin() != null ? (entity.getPreCheckin().getEconomicNumber() != null && !entity.getPreCheckin().getEconomicNumber().isBlank() ? entity.getPreCheckin().getEconomicNumber() : entity.getPreCheckin().getNoEcoTractor()) : null)")
    @Mapping(target = "noEcoTractor", expression = "java(entity.getPreCheckin() != null ? (entity.getPreCheckin().getNoEcoTractor() != null && !entity.getPreCheckin().getNoEcoTractor().isBlank() ? entity.getPreCheckin().getNoEcoTractor() : entity.getPreCheckin().getEconomicNumber()) : null)")
    @Mapping(target = "boxEconomicNumber", expression = "java(entity.getPreCheckin() != null ? (entity.getPreCheckin().getBoxEconomicNumber() != null && !entity.getPreCheckin().getBoxEconomicNumber().isBlank() ? entity.getPreCheckin().getBoxEconomicNumber() : null) : null)")
    @Mapping(target = "noEcoCaja", expression = "java(entity.getPreCheckin() != null ? (entity.getPreCheckin().getBoxEconomicNumber() != null && !entity.getPreCheckin().getBoxEconomicNumber().isBlank() ? entity.getPreCheckin().getBoxEconomicNumber() : null) : null)")
    @Mapping(target = "totalPallets", expression = "java(entity.getPallets() != null ? entity.getPallets().size() : 0)")
    @Mapping(target = "totalPieces", expression = "java(entity.getPallets() != null ? entity.getPallets().stream().mapToDouble(p -> p.getPieces() != null ? p.getPieces().doubleValue() : 0.0).sum() : 0.0)")
    ReceptionSummaryResponse toSummaryResponse(WarehouseReceptionEntity entity);

    @Mapping(source = "sku.id", target = "skuId")
    @Mapping(source = "sku.code", target = "skuCode")
    @Mapping(source = "sku.name", target = "productName")
    @Mapping(source = "reception.id", target = "receptionId")
    @Mapping(target = "palletsCount", ignore = true)
    @Mapping(target = "piecesCount", ignore = true)
    com.fourguard.wms.application.dto.response.reception.ReceptionLotResponse toLotResponse(com.fourguard.wms.infrastructure.persistence.entity.WarehouseReceptionLotEntity entity);

    List<com.fourguard.wms.application.dto.response.reception.ReceptionLotResponse> toLotResponseList(List<com.fourguard.wms.infrastructure.persistence.entity.WarehouseReceptionLotEntity> entities);

    @Mapping(source = "sku.id", target = "skuId")
    @Mapping(source = "sku.code", target = "skuCode")
    @Mapping(source = "sku.name", target = "description")
    @Mapping(source = "supplier.id", target = "supplierId")
    @Mapping(source = "supplier", target = "supplierName", qualifiedByName = "supplierToName")
    @Mapping(source = "palletType", target = "palletTypeId", qualifiedByName = "palletTypeToString")
    @Mapping(source = "palletType", target = "palletTypeLabel", qualifiedByName = "palletTypeToLabel")
    @Mapping(source = "inventoryItem.id", target = "inventoryItemId")
    @Mapping(target = "lotNumber", expression = "java(entity.getLotNumber() != null && !entity.getLotNumber().isBlank() ? entity.getLotNumber() : (entity.getReceptionLot() != null ? entity.getReceptionLot().getLotNumber() : null))")
    @Mapping(target = "expirationDate", expression = "java(entity.getExpirationDate() != null ? entity.getExpirationDate() : (entity.getReceptionLot() != null ? entity.getReceptionLot().getExpirationDate() : null))")
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
        return type != null ? type.getDescription() : null;
    }

    @Named("forkliftOperatorToName")
    default String forkliftOperatorToName(com.fourguard.wms.infrastructure.persistence.entity.ForkliftOperatorEntity operator) {
        if (operator == null) return null;
        if (operator.getFullName() != null && !operator.getFullName().isBlank()) {
            return operator.getFullName();
        }
        return operator.getCode();
    }

    @Named("supplierToName")
    default String supplierToName(com.fourguard.wms.infrastructure.persistence.entity.SupplierEntity supplier) {
        if (supplier == null) return null;
        if (supplier.getCommercialName() != null && !supplier.getCommercialName().isBlank()) {
            return supplier.getCommercialName();
        }
        if (supplier.getLegalName() != null && !supplier.getLegalName().isBlank()) {
            return supplier.getLegalName();
        }
        return supplier.getCode();
    }

    @Named("mapSealsToStrings")
    default List<String> mapSealsToStrings(List<WarehouseReceptionSealEntity> seals) {
        if (seals == null) return List.of();
        return seals.stream().map(s -> s.getSealNumber()).collect(Collectors.toList());
    }
}
