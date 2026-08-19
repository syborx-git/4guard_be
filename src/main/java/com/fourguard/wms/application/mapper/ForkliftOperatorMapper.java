package com.fourguard.wms.application.mapper;

import com.fourguard.wms.application.dto.request.CreateForkliftOperatorRequest;
import com.fourguard.wms.application.dto.request.UpdateForkliftOperatorRequest;
import com.fourguard.wms.application.dto.response.ForkliftOperatorResponse;
import com.fourguard.wms.domain.enums.ForkliftOperatorStatus;
import com.fourguard.wms.domain.enums.LicenseStatus;
import com.fourguard.wms.infrastructure.persistence.entity.ForkliftOperatorEntity;
import org.mapstruct.*;

/**
 * MapStruct mapper for Forklift Operator (HU-142).
 *
 * <p>Uses {@code @BeanMapping(ignoreByDefault = true)} to avoid the ambiguity of the boolean
 * field {@code isDeleted} in {@link ForkliftOperatorEntity} (Lombok generates both
 * {@code isDeleted()} getter and {@code setDeleted()} setter, which causes name-resolution
 * conflicts in MapStruct depending on the mapping direction). Only explicitly declared source
 * mappings are applied; all business-computed fields are set by {@code ForkliftOperatorService}.
 */
@Mapper(componentModel = "spring")
public interface ForkliftOperatorMapper {

    /**
     * Maps a creation request to a new entity.
     * Only the user-supplied data fields are mapped. Business-calculated fields
     * (code, fullName, licenseStatus, status, etc.) are set by the service.
     */
    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "firstName",             source = "firstName")
    @Mapping(target = "lastNamePaternal",      source = "lastNamePaternal")
    @Mapping(target = "lastNameMaternal",      source = "lastNameMaternal")
    @Mapping(target = "licenseNumberDc3",      source = "licenseNumberDc3")
    @Mapping(target = "licenseExpirationDate", source = "licenseExpirationDate")
    ForkliftOperatorEntity toEntity(CreateForkliftOperatorRequest request);

    /**
     * Updates an existing entity with the user-supplied data from the update request.
     */
    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "firstName",             source = "firstName")
    @Mapping(target = "lastNamePaternal",      source = "lastNamePaternal")
    @Mapping(target = "lastNameMaternal",      source = "lastNameMaternal")
    @Mapping(target = "licenseNumberDc3",      source = "licenseNumberDc3")
    @Mapping(target = "licenseExpirationDate", source = "licenseExpirationDate")
    void updateEntityFromDto(UpdateForkliftOperatorRequest request, @MappingTarget ForkliftOperatorEntity entity);

    /**
     * Maps entity to response DTO, flattening the organization, branch, and shift relationships.
     */
    @Mapping(source = "organization.id",   target = "organizationId")
    @Mapping(source = "organization.name", target = "organizationName")
    @Mapping(source = "branch.id",         target = "branchId")
    @Mapping(source = "branch.name",       target = "branchName")
    @Mapping(source = "shift.id",          target = "shiftId")
    @Mapping(source = "licenseStatus",     target = "licenseStatus", qualifiedByName = "licenseStatusToString")
    @Mapping(source = "status",            target = "status",        qualifiedByName = "operatorStatusToString")
    ForkliftOperatorResponse toResponse(ForkliftOperatorEntity entity);

    @Named("licenseStatusToString")
    default String licenseStatusToString(LicenseStatus status) {
        return status != null ? status.name() : null;
    }

    @Named("operatorStatusToString")
    default String operatorStatusToString(ForkliftOperatorStatus status) {
        return status != null ? status.name() : null;
    }
}
