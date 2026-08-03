package com.fourguard.wms.application.usecase;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fourguard.wms.application.dto.request.CreateCurrencyRequest;
import com.fourguard.wms.application.dto.request.UpdateCurrencyRequest;
import com.fourguard.wms.application.dto.request.UpdateCurrencyStatusRequest;
import com.fourguard.wms.application.dto.response.CurrencyResponse;
import com.fourguard.wms.application.mapper.CurrencyMapper;
import com.fourguard.wms.domain.enums.CurrencyAuditAction;
import com.fourguard.wms.domain.enums.CurrencyAuditEntityType;
import com.fourguard.wms.domain.enums.CurrencyStatus;
import com.fourguard.wms.domain.exception.ConflictException;
import com.fourguard.wms.domain.exception.CurrencyNotFoundException;
import com.fourguard.wms.domain.model.Currency;
import com.fourguard.wms.domain.model.CurrencyExchangeAudit;
import com.fourguard.wms.domain.ports.in.CurrencyUseCase;
import com.fourguard.wms.domain.ports.out.CurrencyExchangeAuditRepositoryPort;
import com.fourguard.wms.domain.ports.out.CurrencyRepositoryPort;
import com.fourguard.wms.shared.audit.SecurityAuditHelper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class CurrencyService implements CurrencyUseCase {

    private final CurrencyRepositoryPort currencyRepositoryPort;
    private final CurrencyExchangeAuditRepositoryPort auditRepositoryPort;
    private final CurrencyMapper currencyMapper;
    private final SecurityAuditHelper securityAuditHelper;
    private final ObjectMapper objectMapper;

    @Override
    @Transactional(readOnly = true)
    public List<CurrencyResponse> getCurrencies(UUID organizationId) {
        log.debug("Fetching currencies for organizationId: {}", organizationId);
        List<Currency> currencies = currencyRepositoryPort.findAllByOrganizationId(organizationId);
        return currencyMapper.toResponseList(currencies);
    }

    @Override
    @Transactional(readOnly = true)
    public CurrencyResponse getCurrencyById(UUID id) {
        log.debug("Fetching currency by id: {}", id);
        Currency currency = currencyRepositoryPort.findById(id)
                .orElseThrow(() -> new CurrencyNotFoundException("Divisa no encontrada con ID: " + id));
        return currencyMapper.toResponse(currency);
    }

    @Override
    @Transactional
    public CurrencyResponse createCurrency(CreateCurrencyRequest request) {
        log.info("Creating currency: {} for organization: {}", request.getCode(), request.getOrganizationId());
        String currentUser = securityAuditHelper.getCurrentUsername();

        String codeUpper = request.getCode().toUpperCase();
        if (currencyRepositoryPort.existsByOrganizationIdAndCode(request.getOrganizationId(), codeUpper)) {
            throw new ConflictException("Ya existe una divisa registrada con el código '" + codeUpper + "' en la organización.");
        }

        boolean isBase = Boolean.TRUE.equals(request.getIsBase());
        if (isBase) {
            unsetCurrentBaseCurrency(request.getOrganizationId(), currentUser);
        }

        Currency currency = currencyMapper.toDomain(request);
        currency.setCode(codeUpper);
        currency.setIsBase(isBase);
        currency.setStatus(CurrencyStatus.ACTIVE);
        currency.setCreatedAt(OffsetDateTime.now());
        currency.setCreatedBy(currentUser);
        currency.setUpdatedAt(OffsetDateTime.now());
        currency.setUpdatedBy(currentUser);

        Currency saved = currencyRepositoryPort.save(currency);

        logAudit(saved.getOrganizationId(), CurrencyAuditEntityType.CURRENCY, saved.getId(),
                CurrencyAuditAction.CREATED, "Creación de nueva divisa: " + saved.getCode(),
                null, toJson(saved), currentUser);

        return currencyMapper.toResponse(saved);
    }

    @Override
    @Transactional
    public CurrencyResponse updateCurrency(UUID id, UpdateCurrencyRequest request) {
        log.info("Updating currency ID: {}", id);
        String currentUser = securityAuditHelper.getCurrentUsername();

        Currency existing = currencyRepositoryPort.findById(id)
                .orElseThrow(() -> new CurrencyNotFoundException("Divisa no encontrada con ID: " + id));

        String previousJson = toJson(existing);

        if (request.getName() != null) existing.setName(request.getName());
        if (request.getSymbol() != null) existing.setSymbol(request.getSymbol());
        if (request.getDecimalPlaces() != null) existing.setDecimalPlaces(request.getDecimalPlaces());

        existing.setUpdatedAt(OffsetDateTime.now());
        existing.setUpdatedBy(currentUser);

        Currency saved = currencyRepositoryPort.save(existing);

        logAudit(saved.getOrganizationId(), CurrencyAuditEntityType.CURRENCY, saved.getId(),
                CurrencyAuditAction.UPDATED, "Actualización de metadatos de divisa: " + saved.getCode(),
                previousJson, toJson(saved), currentUser);

        return currencyMapper.toResponse(saved);
    }

    @Override
    @Transactional
    public CurrencyResponse updateCurrencyStatus(UUID id, UpdateCurrencyStatusRequest request) {
        log.info("Updating status for currency ID: {} to {}", id, request.getStatus());
        String currentUser = securityAuditHelper.getCurrentUsername();

        Currency existing = currencyRepositoryPort.findById(id)
                .orElseThrow(() -> new CurrencyNotFoundException("Divisa no encontrada con ID: " + id));

        if (Boolean.TRUE.equals(existing.getIsBase()) && request.getStatus() == CurrencyStatus.INACTIVE) {
            throw new ConflictException("Imposible inactivar la divisa base contable de la organización ('" + existing.getCode() + "').");
        }

        String previousJson = toJson(existing);
        existing.setStatus(request.getStatus());
        existing.setUpdatedAt(OffsetDateTime.now());
        existing.setUpdatedBy(currentUser);

        Currency saved = currencyRepositoryPort.save(existing);

        logAudit(saved.getOrganizationId(), CurrencyAuditEntityType.CURRENCY, saved.getId(),
                CurrencyAuditAction.STATUS_CHANGED, "Cambio de estatus de divisa " + saved.getCode() + " a " + saved.getStatus(),
                previousJson, toJson(saved), currentUser);

        return currencyMapper.toResponse(saved);
    }

    @Override
    @Transactional
    public CurrencyResponse setBaseCurrency(UUID id) {
        log.info("Setting base currency ID: {}", id);
        String currentUser = securityAuditHelper.getCurrentUsername();

        Currency target = currencyRepositoryPort.findById(id)
                .orElseThrow(() -> new CurrencyNotFoundException("Divisa no encontrada con ID: " + id));

        if (target.getStatus() == CurrencyStatus.INACTIVE) {
            throw new ConflictException("No se puede establecer una divisa inactiva ('" + target.getCode() + "') como divisa base.");
        }

        if (Boolean.TRUE.equals(target.getIsBase())) {
            return currencyMapper.toResponse(target);
        }

        unsetCurrentBaseCurrency(target.getOrganizationId(), currentUser);

        String previousJson = toJson(target);
        target.setIsBase(true);
        target.setUpdatedAt(OffsetDateTime.now());
        target.setUpdatedBy(currentUser);

        Currency saved = currencyRepositoryPort.save(target);

        logAudit(saved.getOrganizationId(), CurrencyAuditEntityType.CURRENCY, saved.getId(),
                CurrencyAuditAction.SET_BASE, "Establecida como divisa base principal de la organización: " + saved.getCode(),
                previousJson, toJson(saved), currentUser);

        return currencyMapper.toResponse(saved);
    }

    private void unsetCurrentBaseCurrency(UUID organizationId, String currentUser) {
        Optional<Currency> currentBaseOpt = currencyRepositoryPort.findBaseCurrencyByOrganizationId(organizationId);
        if (currentBaseOpt.isPresent()) {
            Currency currentBase = currentBaseOpt.get();
            String prevJson = toJson(currentBase);
            currentBase.setIsBase(false);
            currentBase.setUpdatedAt(OffsetDateTime.now());
            currentBase.setUpdatedBy(currentUser);
            Currency updatedBase = currencyRepositoryPort.save(currentBase);

            logAudit(organizationId, CurrencyAuditEntityType.CURRENCY, updatedBase.getId(),
                    CurrencyAuditAction.SET_BASE, "Divisa " + updatedBase.getCode() + " desmarcada como divisa base",
                    prevJson, toJson(updatedBase), currentUser);
        }
    }

    private void logAudit(UUID orgId, CurrencyAuditEntityType entityType, UUID entityId,
                          CurrencyAuditAction action, String description,
                          String prevValue, String newValue, String user) {
        try {
            CurrencyExchangeAudit audit = CurrencyExchangeAudit.builder()
                    .organizationId(orgId)
                    .entityType(entityType)
                    .entityId(entityId)
                    .action(action)
                    .description(description)
                    .previousValue(prevValue)
                    .newValue(newValue)
                    .performedBy(user != null ? user : "SYSTEM")
                    .performedAt(OffsetDateTime.now())
                    .build();
            auditRepositoryPort.save(audit);
        } catch (Exception e) {
            log.error("Failed to save currency audit record", e);
        }
    }

    private String toJson(Object obj) {
        if (obj == null) return null;
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (Exception e) {
            return "{}";
        }
    }
}
