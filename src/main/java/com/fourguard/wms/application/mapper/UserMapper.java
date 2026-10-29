package com.fourguard.wms.application.mapper;

import com.fourguard.wms.application.dto.UserCreateRequest;
import com.fourguard.wms.application.dto.UserResponse;
import com.fourguard.wms.application.dto.UserUpdateRequest;
import com.fourguard.wms.application.dto.response.auth.UserInfoResponse;
import com.fourguard.wms.domain.model.Branch;
import com.fourguard.wms.domain.model.Organization;
import com.fourguard.wms.domain.model.Role;
import com.fourguard.wms.domain.model.User;
import com.fourguard.wms.infrastructure.persistence.entity.UserEntity;
import com.fourguard.wms.infrastructure.persistence.entity.OrganizationEntity;
import com.fourguard.wms.infrastructure.persistence.entity.BranchEntity;
import com.fourguard.wms.infrastructure.persistence.entity.RoleEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.Named;
import org.mapstruct.BeanMapping;
import org.mapstruct.NullValuePropertyMappingStrategy;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * MapStruct mapper for {@link UserEntity} → response DTOs.
 * MapStruct is configured with Spring injection (componentModel = "spring").
 */
@Mapper(componentModel = "spring")
public interface UserMapper {

    // ── DTO to Domain Model ───────────────────────────────────────────────────

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "password", source = "password") // Password will be hashed in service
    @Mapping(target = "organization", ignore = true) // Resolved in service
    @Mapping(target = "branch", ignore = true)       // Resolved in service
    @Mapping(target = "role", ignore = true)         // Resolved in service
    @Mapping(target = "changePasswordRequired", ignore = true)
    @Mapping(target = "lastLogin", ignore = true)
    @Mapping(target = "failedAttempts", ignore = true)
    @Mapping(target = "lockedUntil", ignore = true)
    @Mapping(target = "permanentlyLocked", ignore = true)
    @Mapping(target = "version", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "updatedBy", ignore = true)
    User toUser(UserCreateRequest request);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "organization", ignore = true) // Resolved in service
    @Mapping(target = "branch", ignore = true)       // Resolved in service
    @Mapping(target = "role", ignore = true)         // Resolved in service
    @Mapping(target = "changePasswordRequired", ignore = true)
    @Mapping(target = "lastLogin", ignore = true)
    @Mapping(target = "failedAttempts", ignore = true)
    @Mapping(target = "lockedUntil", ignore = true)
    @Mapping(target = "permanentlyLocked", ignore = true)
    @Mapping(target = "version", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "updatedBy", ignore = true)
    void updateUserFromDto(UserUpdateRequest request, @MappingTarget User user);

    // ── Domain Model to DTO ───────────────────────────────────────────────────

    @Mapping(target = "organizationId", source = "organization.id")
    @Mapping(target = "organizationName", source = "organization.name")
    @Mapping(target = "branchId", source = "branch.id")
    @Mapping(target = "branchName", source = "branch.name")
    @Mapping(target = "roleId", source = "role.id")
    @Mapping(target = "roleName", source = "role.name")
    UserResponse toUserResponse(User user);

    List<UserResponse> toUserResponseList(List<User> users);

    // ── Domain Model to Entity ────────────────────────────────────────────────

    @Mapping(target = "organization", source = "organization", qualifiedByName = "mapOrganizationToOrganizationEntity")
    @Mapping(target = "branch", source = "branch", qualifiedByName = "mapBranchToBranchEntity")
    @Mapping(target = "role", source = "role", qualifiedByName = "mapRoleToRoleEntity")
    UserEntity toUserEntity(User user);

    // ── Entity to Domain Model ────────────────────────────────────────────────

    @Mapping(target = "organization", source = "organization.id", qualifiedByName = "mapOrganizationIdToOrganization")
    @Mapping(target = "branch", source = "branch.id", qualifiedByName = "mapBranchIdToBranch")
    @Mapping(target = "role", source = "role.id", qualifiedByName = "mapRoleIdToRole")
    User toUser(UserEntity userEntity);

    List<User> toUserList(List<UserEntity> userEntities);

    // ── Existing UserEntity to UserInfoResponse (Auth) ────────────────────────

    /**
     * Maps a UserEntity to a UserInfoResponse.
     * Full name is computed from first + last name.
     * Permissions are extracted from the EAGER-loaded role.
     */
    @Mapping(target = "fullName",          expression = "java(buildFullName(entity))")
    @Mapping(target = "role",              source  = "role.name")
    @Mapping(target = "roleLevel",         source  = "role.level")
    @Mapping(target = "permissions",       expression = "java(extractPermissions(entity))")
    @Mapping(target = "organizationId",   source = "organization.id")
    @Mapping(target = "organizationName", source = "organization.name")
    @Mapping(target = "branchId",         source = "branch.id")
    @Mapping(target = "branchName",       source = "branch.name")
    @Mapping(target = "forkliftOperatorId", ignore = true)
    @Mapping(target = "employeeCode",      ignore = true)
    UserInfoResponse toUserInfoResponse(UserEntity entity);

    // ── Default helpers ───────────────────────────────────────────────────────

    default String buildFullName(UserEntity entity) {
        String first = entity.getFirstName() != null ? entity.getFirstName() : "";
        String last  = entity.getLastName()  != null ? entity.getLastName()  : "";
        return (first + " " + last).trim();
    }

    default List<String> extractPermissions(UserEntity entity) {
        if (entity.getRole() == null || entity.getRole().getPermissions() == null) {
            return List.of();
        }
        return entity.getRole().getPermissions().stream()
                .map(p -> p.getName())
                .collect(Collectors.toList());
    }

    // These methods are placeholders. The actual resolution of Organization, Branch, Role
    // objects from their IDs will happen in the service layer, or by injecting
    // repository ports into the mapper if MapStruct is configured for that.
    // For now, they return null, and the service will populate them.
    @Named("mapOrganizationIdToOrganization")
    default Organization mapOrganizationIdToOrganization(UUID organizationId) {
        if (organizationId == null) return null;
        return Organization.builder().id(organizationId).build();
    }

    @Named("mapBranchIdToBranch")
    default Branch mapBranchIdToBranch(UUID branchId) {
        if (branchId == null) return null;
        return Branch.builder().id(branchId).build();
    }

    @Named("mapRoleIdToRole")
    default Role mapRoleIdToRole(UUID roleId) {
        if (roleId == null) return null;
        return Role.builder().id(roleId).build();
    }

    @Named("mapOrganizationToOrganizationEntity")
    default OrganizationEntity mapOrganizationToOrganizationEntity(Organization organization) {
        if (organization == null || organization.getId() == null) return null;
        return OrganizationEntity.builder()
                .id(organization.getId())
                .name(organization.getName())
                .version(organization.getVersion())
                .build();
    }

    @Named("mapBranchToBranchEntity")
    default BranchEntity mapBranchToBranchEntity(Branch branch) {
        if (branch == null || branch.getId() == null) return null;
        return BranchEntity.builder()
                .id(branch.getId())
                .name(branch.getName())
                .version(branch.getVersion())
                .build();
    }

    @Named("mapRoleToRoleEntity")
    default RoleEntity mapRoleToRoleEntity(Role role) {
        if (role == null || role.getId() == null) return null;
        return RoleEntity.builder()
                .id(role.getId())
                .name(role.getName())
                .version(role.getVersion())
                .build();
    }
}