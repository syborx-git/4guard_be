package com.fourguard.wms.application.mapper;

import com.fourguard.wms.application.dto.common.ClientContactDto;
import com.fourguard.wms.application.dto.common.PhysicalDestinationDto;
import com.fourguard.wms.application.dto.request.CreateClientRequest;
import com.fourguard.wms.application.dto.request.UpdateClientRequest;
import com.fourguard.wms.application.dto.response.ClientResponse;
import com.fourguard.wms.infrastructure.persistence.entity.ClientContactEntity;
import com.fourguard.wms.infrastructure.persistence.entity.ClientDestinationEntity;
import com.fourguard.wms.infrastructure.persistence.entity.ClientEntity;
import org.mapstruct.*;

import java.util.List;

/**
 * Mapper MapStruct — Clientes, Contactos Corporativos y Destinos Físicos.
 *
 * Estrategia de actualización de colecciones:
 * - Crear: se instancian entidades nuevas con los datos del DTO.
 * - Actualizar: ClientService.java sincroniza las colecciones manualmente
 *   (orphanRemoval = true garantiza que JPA elimine los huérfanos al salvar).
 */
@Mapper(componentModel = "spring", nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
public interface ClientMapper {

    // ── CreateClientRequest → ClientEntity ───────────────────────────────────

    @Mapping(target = "id",              ignore = true)
    @Mapping(target = "organization",    ignore = true)
    @Mapping(target = "contacts",        ignore = true)  // Se sincroniza en servicio
    @Mapping(target = "destinations",    ignore = true)  // Se sincroniza en servicio
    @Mapping(target = "createdAt",       ignore = true)
    @Mapping(target = "updatedAt",       ignore = true)
    @Mapping(target = "createdBy",       ignore = true)
    @Mapping(target = "updatedBy",       ignore = true)
    @Mapping(target = "version",         ignore = true)
    @Mapping(target = "status",          ignore = true)
    ClientEntity toEntity(CreateClientRequest request);

    @AfterMapping
    default void applyDefaultsOnCreate(CreateClientRequest request, @MappingTarget ClientEntity entity) {
        entity.setStatus(request.getStatus() != null ? request.getStatus() : "ACTIVE");
        entity.setVersion(request.getVersion() != null ? request.getVersion() : 1L);
    }

    // ── UpdateClientRequest → ClientEntity (patch) ───────────────────────────

    @Mapping(target = "organization",    ignore = true)
    @Mapping(target = "contacts",        ignore = true)  // Se sincroniza en servicio
    @Mapping(target = "destinations",    ignore = true)  // Se sincroniza en servicio
    @Mapping(target = "createdAt",       ignore = true)
    @Mapping(target = "updatedAt",       ignore = true)
    @Mapping(target = "createdBy",       ignore = true)
    @Mapping(target = "updatedBy",       ignore = true)
    @Mapping(target = "version",         ignore = true)
    @Mapping(target = "status",          ignore = true)
    void updateEntityFromDto(UpdateClientRequest request, @MappingTarget ClientEntity entity);

    @AfterMapping
    default void applyConditionalFieldsOnUpdate(UpdateClientRequest request, @MappingTarget ClientEntity entity) {
        if (request.getStatus() != null && !request.getStatus().isBlank()) {
            entity.setStatus(request.getStatus());
        }
        if (request.getVersion() != null) {
            entity.setVersion(request.getVersion());
        }
    }

    // ── ClientEntity → ClientResponse ────────────────────────────────────────

    @Mapping(source = "organization.id",   target = "organizationId")
    @Mapping(source = "organization.name", target = "organizationName")
    ClientResponse toResponse(ClientEntity entity);

    // ── Colecciones Anidadas ──────────────────────────────────────────────────

    @Mapping(target = "client",     ignore = true)
    @Mapping(target = "createdBy",  ignore = true)
    @Mapping(target = "updatedBy",  ignore = true)
    @Mapping(target = "createdAt",  ignore = true)
    @Mapping(target = "updatedAt",  ignore = true)
    ClientContactEntity toContactEntity(ClientContactDto dto);

    @Mapping(source = "client.id",  target = "id",        ignore = false)
    ClientContactDto toContactDto(ClientContactEntity entity);

    @Mapping(target = "client",     ignore = true)
    @Mapping(target = "createdBy",  ignore = true)
    @Mapping(target = "updatedBy",  ignore = true)
    @Mapping(target = "createdAt",  ignore = true)
    @Mapping(target = "updatedAt",  ignore = true)
    ClientDestinationEntity toDestinationEntity(PhysicalDestinationDto dto);

    PhysicalDestinationDto toDestinationDto(ClientDestinationEntity entity);

    List<ClientContactDto>       toContactDtoList(List<ClientContactEntity> entities);
    List<PhysicalDestinationDto> toDestinationDtoList(List<ClientDestinationEntity> entities);
}
