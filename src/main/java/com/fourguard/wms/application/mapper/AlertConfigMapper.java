package com.fourguard.wms.application.mapper;

import com.fourguard.wms.application.dto.request.CreateAlertConfigRequest;
import com.fourguard.wms.application.dto.request.UpdateAlertConfigRequest;
import com.fourguard.wms.application.dto.response.AlertConfigResponse;
import com.fourguard.wms.application.dto.response.AlertFiredEventResponse;
import com.fourguard.wms.infrastructure.persistence.entity.AlertConfigEntity;
import com.fourguard.wms.infrastructure.persistence.entity.AlertFiredEventEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring")
public interface AlertConfigMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "organization", ignore = true)
    @Mapping(target = "isDeleted", ignore = true)
    @Mapping(target = "deletedAt", ignore = true)
    @Mapping(target = "version", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "updatedBy", ignore = true)
    AlertConfigEntity toEntity(CreateAlertConfigRequest request);

    @Mapping(source = "organization.id", target = "organizationId")
    AlertConfigResponse toResponse(AlertConfigEntity entity);

    @Mapping(source = "alertConfiguration.id", target = "alertConfigurationId")
    @Mapping(source = "organization.id", target = "organizationId")
    @Mapping(source = "branch.id", target = "branchId")
    AlertFiredEventResponse toFiredEventResponse(AlertFiredEventEntity entity);

    @Mapping(target = "organization", ignore = true)
    @Mapping(target = "isDeleted", ignore = true)
    @Mapping(target = "deletedAt", ignore = true)
    @Mapping(target = "version", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "updatedBy", ignore = true)
    void updateEntityFromDto(UpdateAlertConfigRequest request, @MappingTarget AlertConfigEntity entity);
}
