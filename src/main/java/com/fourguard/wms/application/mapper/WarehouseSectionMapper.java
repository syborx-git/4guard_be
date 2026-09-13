package com.fourguard.wms.application.mapper;

import com.fourguard.wms.application.dto.request.CreateWarehouseSectionRequest;
import com.fourguard.wms.application.dto.request.UpdateWarehouseSectionRequest;
import com.fourguard.wms.application.dto.response.WarehouseSectionResponse;
import com.fourguard.wms.domain.enums.WarehouseSectionStatus;
import com.fourguard.wms.infrastructure.persistence.entity.WarehouseSectionEntity;
import org.mapstruct.*;

@Mapper(componentModel = "spring")
public interface WarehouseSectionMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "branch", ignore = true)
    @Mapping(target = "locations", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "updatedBy", ignore = true)
    @Mapping(target = "version", ignore = true)
    WarehouseSectionEntity toEntity(CreateWarehouseSectionRequest request);

    @Mapping(source = "branch.id", target = "branchId")
    @Mapping(source = "branch.name", target = "branchName")
    @Mapping(target = "status", expression = "java(entity.getStatus() != null ? entity.getStatus().name() : com.fourguard.wms.domain.enums.WarehouseSectionStatus.ACTIVE.name())")
    WarehouseSectionResponse toResponse(WarehouseSectionEntity entity);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "branch", ignore = true)
    @Mapping(target = "locations", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "updatedBy", ignore = true)
    @Mapping(target = "version", ignore = true)
    void updateEntityFromDto(UpdateWarehouseSectionRequest request, @MappingTarget WarehouseSectionEntity entity);
}


