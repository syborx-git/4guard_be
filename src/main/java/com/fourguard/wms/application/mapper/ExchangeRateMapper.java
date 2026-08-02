package com.fourguard.wms.application.mapper;

import com.fourguard.wms.application.dto.request.CreateExchangeRateRequest;
import com.fourguard.wms.application.dto.response.ExchangeRateResponse;
import com.fourguard.wms.domain.model.ExchangeRate;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring")
public interface ExchangeRateMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "fromCurrencyCode", ignore = true)
    @Mapping(target = "toCurrencyCode", ignore = true)
    @Mapping(target = "inverseRate", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "updatedBy", ignore = true)
    ExchangeRate toDomain(CreateExchangeRateRequest request);

    ExchangeRateResponse toResponse(ExchangeRate domain);

    List<ExchangeRateResponse> toResponseList(List<ExchangeRate> domainList);
}
