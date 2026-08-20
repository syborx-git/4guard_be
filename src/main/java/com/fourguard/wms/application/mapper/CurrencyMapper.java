package com.fourguard.wms.application.mapper;

import com.fourguard.wms.application.dto.request.CreateCurrencyRequest;
import com.fourguard.wms.application.dto.response.CurrencyResponse;
import com.fourguard.wms.domain.model.Currency;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring")
public interface CurrencyMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "updatedBy", ignore = true)
    Currency toDomain(CreateCurrencyRequest request);

    CurrencyResponse toResponse(Currency domain);

    List<CurrencyResponse> toResponseList(List<Currency> domainList);
}
