package com.fourguard.wms.application.mapper;

import com.fourguard.wms.application.dto.response.CurrencyAuditResponse;
import com.fourguard.wms.domain.model.CurrencyExchangeAudit;
import org.mapstruct.Mapper;

import java.util.List;

@Mapper(componentModel = "spring")
public interface CurrencyExchangeAuditMapper {

    CurrencyAuditResponse toResponse(CurrencyExchangeAudit domain);

    List<CurrencyAuditResponse> toResponseList(List<CurrencyExchangeAudit> domainList);
}
