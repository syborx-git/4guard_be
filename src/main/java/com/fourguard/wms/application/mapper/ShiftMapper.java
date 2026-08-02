package com.fourguard.wms.application.mapper;

import com.fourguard.wms.application.dto.request.CreateShiftRequest;
import com.fourguard.wms.application.dto.request.UpdateShiftRequest;
import com.fourguard.wms.application.dto.response.ShiftResponse;
import com.fourguard.wms.infrastructure.persistence.entity.ShiftEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring")
public interface ShiftMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "branch", ignore = true)
    @Mapping(target = "warehouseSection", ignore = true)
    @Mapping(target = "isOvernight", ignore = true)
    @Mapping(target = "netDurationMinutes", ignore = true)
    @Mapping(target = "isDeleted", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "updatedBy", ignore = true)
    @Mapping(target = "version", ignore = true)
    ShiftEntity toEntity(CreateShiftRequest request);

    @Mapping(source = "branch.id", target = "branchId")
    @Mapping(source = "branch.name", target = "branchName")
    @Mapping(source = "warehouseSection.id", target = "warehouseSectionId")
    @Mapping(source = "warehouseSection.name", target = "warehouseSectionName")
    ShiftResponse toResponse(ShiftEntity entity);

    @Mapping(target = "branch", ignore = true)
    @Mapping(target = "warehouseSection", ignore = true)
    @Mapping(target = "isOvernight", ignore = true)
    @Mapping(target = "netDurationMinutes", ignore = true)
    @Mapping(target = "isDeleted", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "updatedBy", ignore = true)
    @Mapping(target = "version", ignore = true)
    void updateEntityFromDto(UpdateShiftRequest request, @MappingTarget ShiftEntity entity);
}
