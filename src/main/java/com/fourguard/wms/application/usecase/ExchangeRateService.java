package com.fourguard.wms.application.usecase;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fourguard.wms.application.dto.request.ConvertCurrencyRequest;
import com.fourguard.wms.application.dto.request.CreateExchangeRateRequest;
import com.fourguard.wms.application.dto.response.BanxicoLiveRateResponse;
import com.fourguard.wms.application.dto.response.ConvertCurrencyResponse;
import com.fourguard.wms.application.dto.response.ExchangeRateResponse;
import com.fourguard.wms.application.dto.response.ParityMatrixResponse;
import com.fourguard.wms.application.mapper.CurrencyMapper;
import com.fourguard.wms.application.mapper.ExchangeRateMapper;
import com.fourguard.wms.domain.enums.CurrencyAuditAction;
import com.fourguard.wms.domain.enums.CurrencyAuditEntityType;
import com.fourguard.wms.domain.enums.ExchangeRateStatus;
import com.fourguard.wms.domain.exception.CurrencyNotFoundException;
import com.fourguard.wms.domain.exception.ValidationException;
import com.fourguard.wms.domain.model.Currency;
import com.fourguard.wms.domain.model.CurrencyExchangeAudit;
import com.fourguard.wms.domain.model.ExchangeRate;
import com.fourguard.wms.domain.ports.in.ExchangeRateUseCase;
import com.fourguard.wms.domain.ports.out.CurrencyExchangeAuditRepositoryPort;
import com.fourguard.wms.domain.ports.out.CurrencyRepositoryPort;
import com.fourguard.wms.domain.ports.out.ExchangeRateRepositoryPort;
import com.fourguard.wms.shared.audit.SecurityAuditHelper;
import com.fourguard.wms.domain.enums.BanxicoSeries;
import com.fourguard.wms.domain.enums.ExchangeRateSourceType;
import com.fourguard.wms.domain.ports.out.BanxicoExchangeRatePort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class ExchangeRateService implements ExchangeRateUseCase {

    private final ExchangeRateRepositoryPort exchangeRateRepositoryPort;
    private final CurrencyRepositoryPort currencyRepositoryPort;
    private final CurrencyExchangeAuditRepositoryPort auditRepositoryPort;
    private final BanxicoExchangeRatePort banxicoExchangeRatePort;
    private final ExchangeRateMapper exchangeRateMapper;
    private final CurrencyMapper currencyMapper;
    private final SecurityAuditHelper securityAuditHelper;
    private final ObjectMapper objectMapper;

    @Override
    @Transactional(readOnly = true)
    public List<ExchangeRateResponse> getExchangeRates(UUID organizationId, String fromCode, String toCode, LocalDate date) {
        log.debug("Fetching exchange rates for organizationId: {}", organizationId);

        UUID fromId = null;
        if (fromCode != null && !fromCode.isBlank()) {
            fromId = currencyRepositoryPort.findByOrganizationIdAndCode(organizationId, fromCode.toUpperCase())
                    .map(Currency::getId).orElse(null);
        }

        UUID toId = null;
        if (toCode != null && !toCode.isBlank()) {
            toId = currencyRepositoryPort.findByOrganizationIdAndCode(organizationId, toCode.toUpperCase())
                    .map(Currency::getId).orElse(null);
        }

        List<ExchangeRate> rates = exchangeRateRepositoryPort.findRatesWithFilters(organizationId, fromId, toId, date);
        return exchangeRateMapper.toResponseList(rates);
    }

    @Override
    @Transactional(readOnly = true)
    public ParityMatrixResponse getLatestParityMatrix(UUID organizationId) {
        log.debug("Fetching parity matrix for organizationId: {}", organizationId);

        Currency baseCurrency = currencyRepositoryPort.findBaseCurrencyByOrganizationId(organizationId)
                .orElse(null);

        List<ExchangeRate> activeRates = exchangeRateRepositoryPort.findActiveRatesByOrganizationId(organizationId);

        return ParityMatrixResponse.builder()
                .organizationId(organizationId)
                .baseCurrency(currencyMapper.toResponse(baseCurrency))
                .activeRates(exchangeRateMapper.toResponseList(activeRates))
                .build();
    }

    @Override
    @Transactional
    public ExchangeRateResponse saveExchangeRate(CreateExchangeRateRequest request) {
        log.info("Saving exchange rate for organization: {}", request.getOrganizationId());
        String currentUser = securityAuditHelper.getCurrentUsername();

        if (request.getFromCurrencyId().equals(request.getToCurrencyId())) {
            throw new ValidationException("La divisa de origen y de destino no pueden ser la misma.");
        }

        Currency fromCurrency = currencyRepositoryPort.findById(request.getFromCurrencyId())
                .orElseThrow(() -> new CurrencyNotFoundException("Divisa de origen no encontrada con ID: " + request.getFromCurrencyId()));

        Currency toCurrency = currencyRepositoryPort.findById(request.getToCurrencyId())
                .orElseThrow(() -> new CurrencyNotFoundException("Divisa de destino no encontrada con ID: " + request.getToCurrencyId()));

        BigDecimal rate = request.getRate();
        BigDecimal inverseRate = BigDecimal.ONE.divide(rate, 6, RoundingMode.HALF_UP);

        ExchangeRate exchangeRate = exchangeRateMapper.toDomain(request);
        exchangeRate.setFromCurrencyCode(fromCurrency.getCode());
        exchangeRate.setToCurrencyCode(toCurrency.getCode());
        exchangeRate.setInverseRate(inverseRate);
        exchangeRate.setStatus(ExchangeRateStatus.ACTIVE);
        exchangeRate.setCreatedAt(OffsetDateTime.now());
        exchangeRate.setCreatedBy(currentUser);
        exchangeRate.setUpdatedAt(OffsetDateTime.now());
        exchangeRate.setUpdatedBy(currentUser);

        ExchangeRate saved = exchangeRateRepositoryPort.save(exchangeRate);

        logAudit(saved.getOrganizationId(), CurrencyAuditEntityType.EXCHANGE_RATE, saved.getId(),
                CurrencyAuditAction.RATE_CHANGED,
                String.format("Registro de tipo de cambio %s -> %s = %s", fromCurrency.getCode(), toCurrency.getCode(), rate),
                null, toJson(saved), currentUser);

        return exchangeRateMapper.toResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public ConvertCurrencyResponse convert(ConvertCurrencyRequest request) {
        log.debug("Executing real-time conversion for organizationId: {}", request.getOrganizationId());

        Currency fromCurrency = resolveCurrency(request.getOrganizationId(), request.getFromCurrencyId(), request.getFromCode(), "origen");
        Currency toCurrency = resolveCurrency(request.getOrganizationId(), request.getToCurrencyId(), request.getToCode(), "destino");

        BigDecimal amount = request.getAmount();
        LocalDate queryDate = request.getDate() != null ? request.getDate() : LocalDate.now();

        if (fromCurrency.getId().equals(toCurrency.getId())) {
            return ConvertCurrencyResponse.builder()
                    .fromCurrencyId(fromCurrency.getId())
                    .fromCode(fromCurrency.getCode())
                    .toCurrencyId(toCurrency.getId())
                    .toCode(toCurrency.getCode())
                    .originalAmount(amount)
                    .convertedAmount(amount)
                    .rateUsed(BigDecimal.ONE.setScale(6, RoundingMode.HALF_UP))
                    .effectiveDate(queryDate)
                    .conversionPath("SAME_CURRENCY")
                    .build();
        }

        // 1. Check direct rate (from -> to)
        Optional<ExchangeRate> directRateOpt = exchangeRateRepositoryPort.findLatestRate(
                request.getOrganizationId(), fromCurrency.getId(), toCurrency.getId(), queryDate);

        if (directRateOpt.isPresent()) {
            ExchangeRate rateObj = directRateOpt.get();
            BigDecimal rateUsed = rateObj.getRate();
            BigDecimal convertedAmount = amount.multiply(rateUsed).setScale(toCurrency.getDecimalPlaces(), RoundingMode.HALF_UP);

            return ConvertCurrencyResponse.builder()
                    .fromCurrencyId(fromCurrency.getId())
                    .fromCode(fromCurrency.getCode())
                    .toCurrencyId(toCurrency.getId())
                    .toCode(toCurrency.getCode())
                    .originalAmount(amount)
                    .convertedAmount(convertedAmount)
                    .rateUsed(rateUsed)
                    .effectiveDate(rateObj.getEffectiveDate())
                    .conversionPath(String.format("DIRECT (%s -> %s)", fromCurrency.getCode(), toCurrency.getCode()))
                    .build();
        }

        // 2. Check inverse rate (to -> from)
        Optional<ExchangeRate> inverseRateOpt = exchangeRateRepositoryPort.findLatestRate(
                request.getOrganizationId(), toCurrency.getId(), fromCurrency.getId(), queryDate);

        if (inverseRateOpt.isPresent()) {
            ExchangeRate rateObj = inverseRateOpt.get();
            BigDecimal rateUsed = rateObj.getInverseRate();
            BigDecimal convertedAmount = amount.multiply(rateUsed).setScale(toCurrency.getDecimalPlaces(), RoundingMode.HALF_UP);

            return ConvertCurrencyResponse.builder()
                    .fromCurrencyId(fromCurrency.getId())
                    .fromCode(fromCurrency.getCode())
                    .toCurrencyId(toCurrency.getId())
                    .toCode(toCurrency.getCode())
                    .originalAmount(amount)
                    .convertedAmount(convertedAmount)
                    .rateUsed(rateUsed)
                    .effectiveDate(rateObj.getEffectiveDate())
                    .conversionPath(String.format("INVERSE (%s -> %s)", fromCurrency.getCode(), toCurrency.getCode()))
                    .build();
        }

        // 3. Triangulation via base currency
        Currency baseCurrency = currencyRepositoryPort.findBaseCurrencyByOrganizationId(request.getOrganizationId())
                .orElseThrow(() -> new ValidationException("No existe una divisa base configurada para triangular la conversión."));

        if (!fromCurrency.getId().equals(baseCurrency.getId()) && !toCurrency.getId().equals(baseCurrency.getId())) {
            BigDecimal fromToBaseRate = resolveRate(request.getOrganizationId(), fromCurrency.getId(), baseCurrency.getId(), queryDate);
            BigDecimal baseToTargetRate = resolveRate(request.getOrganizationId(), baseCurrency.getId(), toCurrency.getId(), queryDate);

            BigDecimal combinedRate = fromToBaseRate.multiply(baseToTargetRate).setScale(6, RoundingMode.HALF_UP);
            BigDecimal convertedAmount = amount.multiply(combinedRate).setScale(toCurrency.getDecimalPlaces(), RoundingMode.HALF_UP);

            return ConvertCurrencyResponse.builder()
                    .fromCurrencyId(fromCurrency.getId())
                    .fromCode(fromCurrency.getCode())
                    .toCurrencyId(toCurrency.getId())
                    .toCode(toCurrency.getCode())
                    .originalAmount(amount)
                    .convertedAmount(convertedAmount)
                    .rateUsed(combinedRate)
                    .effectiveDate(queryDate)
                    .conversionPath(String.format("TRIANGULATED (%s -> %s -> %s)", fromCurrency.getCode(), baseCurrency.getCode(), toCurrency.getCode()))
                    .build();
        }

        throw new ValidationException(String.format("No se encontró tipo de cambio vigente para convertir de %s a %s.",
                fromCurrency.getCode(), toCurrency.getCode()));
    }

    private BigDecimal resolveRate(UUID orgId, UUID fromId, UUID toId, LocalDate date) {
        Optional<ExchangeRate> direct = exchangeRateRepositoryPort.findLatestRate(orgId, fromId, toId, date);
        if (direct.isPresent()) return direct.get().getRate();

        Optional<ExchangeRate> inverse = exchangeRateRepositoryPort.findLatestRate(orgId, toId, fromId, date);
        if (inverse.isPresent()) return inverse.get().getInverseRate();

        throw new ValidationException("No se encontró tipo de cambio entre las divisas especificadas.");
    }

    private Currency resolveCurrency(UUID orgId, UUID currencyId, String code, String label) {
        if (currencyId != null) {
            return currencyRepositoryPort.findById(currencyId)
                    .orElseThrow(() -> new CurrencyNotFoundException("Divisa de " + label + " no encontrada con ID: " + currencyId));
        }
        if (code != null && !code.isBlank()) {
            return currencyRepositoryPort.findByOrganizationIdAndCode(orgId, code.toUpperCase())
                    .orElseThrow(() -> new CurrencyNotFoundException("Divisa de " + label + " no encontrada con código: " + code));
        }
        throw new ValidationException("Debe especificar el ID o código de la divisa de " + label + ".");
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
            log.error("Failed to save exchange rate audit record", e);
        }
    }

    @Override
    @Transactional
    public List<ExchangeRateResponse> syncBanxicoRates(UUID organizationId) {
        UUID targetOrgId = organizationId != null ? organizationId : UUID.fromString("a53f0907-9fa5-4bdf-87db-2eb5e7683935");
        log.info("[BANXICO-SYNC] Iniciando sincronización automática con Banxico SIE para organización: {}", targetOrgId);

        Currency baseCurrency = currencyRepositoryPort.findBaseCurrencyByOrganizationId(targetOrgId)
                .orElseThrow(() -> new ValidationException("No existe una divisa base configurada para la organización: " + targetOrgId));

        Map<BanxicoSeries, BigDecimal> latestRates = banxicoExchangeRatePort.fetchAllLatestRates();
        List<ExchangeRate> savedRates = new ArrayList<>();
        String currentUser = securityAuditHelper.getCurrentUsername();
        String auditUser = (currentUser != null && !currentUser.isBlank() && !"anonymousUser".equals(currentUser)) ? currentUser : "SYSTEM_JOB_BANXICO";

        for (Map.Entry<BanxicoSeries, BigDecimal> entry : latestRates.entrySet()) {
            BanxicoSeries series = entry.getKey();
            BigDecimal rate = entry.getValue();

            Optional<Currency> fromCurrencyOpt = currencyRepositoryPort.findByOrganizationIdAndCode(targetOrgId, series.getCurrencyCode());
            if (fromCurrencyOpt.isEmpty()) {
                log.warn("[BANXICO-SYNC] Divisa {} no encontrada en el catálogo de la organización {}", series.getCurrencyCode(), targetOrgId);
                continue;
            }

            Currency fromCurrency = fromCurrencyOpt.get();
            if (fromCurrency.getId().equals(baseCurrency.getId())) {
                continue;
            }

            BigDecimal inverseRate = BigDecimal.ONE.divide(rate, 6, RoundingMode.HALF_UP);

            ExchangeRate exchangeRate = ExchangeRate.builder()
                    .organizationId(targetOrgId)
                    .fromCurrencyId(fromCurrency.getId())
                    .fromCurrencyCode(fromCurrency.getCode())
                    .toCurrencyId(baseCurrency.getId())
                    .toCurrencyCode(baseCurrency.getCode())
                    .rate(rate)
                    .inverseRate(inverseRate)
                    .effectiveDate(LocalDate.now())
                    .sourceType(ExchangeRateSourceType.CENTRAL_BANK)
                    .notes("Sincronización oficial de Banxico SIE (Serie " + series.getSeriesId() + ")")
                    .status(ExchangeRateStatus.ACTIVE)
                    .createdAt(OffsetDateTime.now())
                    .createdBy(auditUser)
                    .updatedAt(OffsetDateTime.now())
                    .updatedBy(auditUser)
                    .build();

            ExchangeRate saved = exchangeRateRepositoryPort.save(exchangeRate);
            savedRates.add(saved);

            logAudit(targetOrgId, CurrencyAuditEntityType.EXCHANGE_RATE, saved.getId(),
                    CurrencyAuditAction.RATE_CHANGED,
                    String.format("Sincronización oficial Banxico %s -> %s = %s (Serie %s)", fromCurrency.getCode(), baseCurrency.getCode(), rate, series.getSeriesId()),
                    null, toJson(saved), auditUser);
        }

        return exchangeRateMapper.toResponseList(savedRates);
    }

    @Override
    @Transactional(readOnly = true)
    public BanxicoLiveRateResponse fetchLiveBanxicoRateBySeries(String seriesId) {
        log.debug("[BANXICO-LIVE] Consultando tasa en tiempo real para serie: {}", seriesId);
        return banxicoExchangeRatePort.fetchLiveRateBySeriesId(seriesId)
                .orElseThrow(() -> new ValidationException("No se pudo obtener la cotización oficial de Banxico para la serie " + seriesId + ". Verifique que el token esté configurado en el archivo .env o que la serie sea válida."));
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
