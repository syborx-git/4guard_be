package com.fourguard.wms.application.mapper;

import com.fourguard.wms.application.dto.request.CreateProductSkuRequest;
import com.fourguard.wms.application.dto.request.UpdateProductSkuRequest;
import com.fourguard.wms.application.dto.response.ProductSkuResponse;
import com.fourguard.wms.infrastructure.persistence.entity.ProductSkuEntity;
import org.mapstruct.*;

@Mapper(componentModel = "spring")
public interface ProductSkuMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "client", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "updatedBy", ignore = true)
    @Mapping(target = "version", ignore = true)
    ProductSkuEntity toEntity(CreateProductSkuRequest request);

    @AfterMapping
    default void applyDefaultsOnCreate(CreateProductSkuRequest request, @MappingTarget ProductSkuEntity entity) {
        if (entity.getStatus() == null || entity.getStatus().isBlank()) {
            entity.setStatus("ACTIVE");
        }
        if (entity.getIsDeleted() == null) {
            entity.setIsDeleted(false);
        }
    }

    @Mapping(source = "client.id", target = "clientId")
    @Mapping(source = "client.name", target = "clientName")
    ProductSkuResponse toResponse(ProductSkuEntity entity);

    @Mapping(target = "client", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "updatedBy", ignore = true)
    @Mapping(target = "version", ignore = true)
    void updateEntityFromDto(UpdateProductSkuRequest request, @MappingTarget ProductSkuEntity entity);
}
