package com.fourguard.wms.application.usecase;

import com.fourguard.wms.application.dto.response.performance.PerformanceMetricsDto.*;
import com.fourguard.wms.infrastructure.persistence.entity.PerformanceKpiEntity;
import com.fourguard.wms.infrastructure.persistence.repository.PerformanceKpiJpaRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class PerformanceKpiManagementService {

    private final PerformanceKpiJpaRepository kpiRepository;

    @Transactional(readOnly = true)
    public List<KpiResponseDto> listKpis(UUID organizationId, String module, String search) {
        List<PerformanceKpiEntity> entities = kpiRepository.findByIsEnabledTrue();

        if (entities.isEmpty()) {
            // Auto-inicializar catálogo base si la tabla está vacía en BD
            seedInitialKpis();
            entities = kpiRepository.findByIsEnabledTrue();
        }

        if (module != null && !module.isBlank()) {
            entities = entities.stream()
                    .filter(k -> module.equalsIgnoreCase(k.getModule()))
                    .collect(Collectors.toList());
        }

        if (search != null && !search.isBlank()) {
            String query = search.toLowerCase().trim();
            entities = entities.stream()
                    .filter(k -> k.getName().toLowerCase().contains(query) ||
                            (k.getDescription() != null && k.getDescription().toLowerCase().contains(query)) ||
                            (k.getSourceProcess() != null && k.getSourceProcess().toLowerCase().contains(query)))
                    .collect(Collectors.toList());
        }

        return entities.stream().map(this::mapToDto).collect(Collectors.toList());
    }

    @Transactional
    public KpiResponseDto createKpi(UUID organizationId, CreateKpiRequestDto dto, String username) {
        PerformanceKpiEntity entity = PerformanceKpiEntity.builder()
                .name(dto.getName())
                .description(dto.getDescription())
                .module(dto.getModule())
                .unit(dto.getUnit())
                .evaluationType(dto.getEvaluationType())
                .targetThreshold(dto.getThresholds() != null ? dto.getThresholds().getTarget() : null)
                .warningThreshold(dto.getThresholds() != null ? dto.getThresholds().getWarning() : null)
                .criticalThreshold(dto.getThresholds() != null ? dto.getThresholds().getCritical() : null)
                .rangeLow(dto.getThresholds() != null ? dto.getThresholds().getRangeLow() : null)
                .rangeHigh(dto.getThresholds() != null ? dto.getThresholds().getRangeHigh() : null)
                .sourceProcess(dto.getSourceConfig() != null ? dto.getSourceConfig().getSourceProcess() : null)
                .startEvent(dto.getSourceConfig() != null ? dto.getSourceConfig().getStartEvent() : null)
                .endEvent(dto.getSourceConfig() != null ? dto.getSourceConfig().getEndEvent() : null)
                .frequencyValue(dto.getSourceConfig() != null ? dto.getSourceConfig().getFrequencyValue() : 15)
                .frequencyUnit(dto.getSourceConfig() != null ? dto.getSourceConfig().getFrequencyUnit() : "MINUTES")
                .isEnabled(true)
                .status("OPTIMAL")
                .createdByUser(username != null ? username : "admin")
                .updatedByUser(username != null ? username : "admin")
                .build();

        PerformanceKpiEntity saved = kpiRepository.save(entity);
        return mapToDto(saved);
    }

    @Transactional
    public KpiResponseDto updateKpi(UUID organizationId, UUID id, UpdateKpiRequestDto dto, String username) {
        PerformanceKpiEntity entity = kpiRepository.findByIdAndIsEnabledTrue(id)
                .orElseThrow(() -> new IllegalArgumentException("KPI no encontrado con ID: " + id));

        entity.setName(dto.getName());
        entity.setDescription(dto.getDescription());
        entity.setModule(dto.getModule());
        entity.setUnit(dto.getUnit());
        entity.setEvaluationType(dto.getEvaluationType());

        if (dto.getThresholds() != null) {
            entity.setTargetThreshold(dto.getThresholds().getTarget());
            entity.setWarningThreshold(dto.getThresholds().getWarning());
            entity.setCriticalThreshold(dto.getThresholds().getCritical());
            entity.setRangeLow(dto.getThresholds().getRangeLow());
            entity.setRangeHigh(dto.getThresholds().getRangeHigh());
        }

        if (dto.getSourceConfig() != null) {
            entity.setSourceProcess(dto.getSourceConfig().getSourceProcess());
            entity.setStartEvent(dto.getSourceConfig().getStartEvent());
            entity.setEndEvent(dto.getSourceConfig().getEndEvent());
            entity.setFrequencyValue(dto.getSourceConfig().getFrequencyValue());
            entity.setFrequencyUnit(dto.getSourceConfig().getFrequencyUnit());
        }

        entity.setUpdatedByUser(username != null ? username : "admin");

        PerformanceKpiEntity saved = kpiRepository.save(entity);
        return mapToDto(saved);
    }

    @Transactional
    public void disableKpi(UUID organizationId, UUID id, String username) {
        PerformanceKpiEntity entity = kpiRepository.findByIdAndIsEnabledTrue(id)
                .orElseThrow(() -> new IllegalArgumentException("KPI no encontrado con ID: " + id));
        entity.setIsEnabled(false);
        entity.setUpdatedByUser(username != null ? username : "admin");
        kpiRepository.save(entity);
    }

    private void seedInitialKpis() {
        List<PerformanceKpiEntity> defaults = new ArrayList<>();
        defaults.add(PerformanceKpiEntity.builder()
                .name("Tiempo de descarga")
                .description("Tiempo promedio desde la llegada del camión hasta que se completa la descarga total de mercancía.")
                .module("RECEIVING")
                .unit("MINUTES")
                .evaluationType("LOWER_IS_BETTER")
                .targetThreshold(45.0)
                .warningThreshold(60.0)
                .criticalThreshold(90.0)
                .currentValue(38.0)
                .lastMeasuredAt(OffsetDateTime.now(ZoneOffset.UTC))
                .status("OPTIMAL")
                .sourceProcess("Recepción")
                .startEvent("Llegada del camión")
                .endEvent("Fin de descarga")
                .frequencyValue(5)
                .frequencyUnit("MINUTES")
                .isEnabled(true)
                .build());

        defaults.add(PerformanceKpiEntity.builder()
                .name("Exactitud de inventario")
                .description("Porcentaje de coincidencia entre el inventario físico y el registrado en el sistema WMS.")
                .module("INVENTORY")
                .unit("PERCENTAGE")
                .evaluationType("HIGHER_IS_BETTER")
                .targetThreshold(99.0)
                .warningThreshold(95.0)
                .criticalThreshold(90.0)
                .currentValue(96.2)
                .lastMeasuredAt(OffsetDateTime.now(ZoneOffset.UTC))
                .status("WARNING")
                .sourceProcess("Inventario cíclico")
                .startEvent("Inicio de conteo cíclico")
                .endEvent("Cierre de conteo cíclico")
                .frequencyValue(1)
                .frequencyUnit("HOURS")
                .isEnabled(true)
                .build());

        defaults.add(PerformanceKpiEntity.builder()
                .name("Ocupación del almacén")
                .description("Porcentaje de ubicaciones ocupadas respecto al total de ubicaciones disponibles.")
                .module("INVENTORY")
                .unit("PERCENTAGE")
                .evaluationType("RANGE")
                .targetThreshold(0.0)
                .warningThreshold(10.0)
                .criticalThreshold(20.0)
                .rangeLow(60.0)
                .rangeHigh(85.0)
                .currentValue(72.0)
                .lastMeasuredAt(OffsetDateTime.now(ZoneOffset.UTC))
                .status("OPTIMAL")
                .sourceProcess("Gestión de ubicaciones")
                .startEvent("Cálculo de ocupación")
                .endEvent("Reporte de ocupación")
                .frequencyValue(30)
                .frequencyUnit("MINUTES")
                .isEnabled(true)
                .build());

        defaults.add(PerformanceKpiEntity.builder()
                .name("Productividad de picking")
                .description("Cantidad de unidades o líneas procesadas por hora por operador.")
                .module("PICKING")
                .unit("UNITS_PER_HOUR")
                .evaluationType("HIGHER_IS_BETTER")
                .targetThreshold(120.0)
                .warningThreshold(90.0)
                .criticalThreshold(60.0)
                .currentValue(115.0)
                .lastMeasuredAt(OffsetDateTime.now(ZoneOffset.UTC))
                .status("OPTIMAL")
                .sourceProcess("Picking")
                .startEvent("Asignación de tarea de picking")
                .endEvent("Confirmación de picking completo")
                .frequencyValue(15)
                .frequencyUnit("MINUTES")
                .isEnabled(true)
                .build());

        defaults.add(PerformanceKpiEntity.builder()
                .name("Tiempo de embarque")
                .description("Tiempo promedio desde el inicio de la carga del camión hasta el cierre del embarque.")
                .module("SHIPPING")
                .unit("MINUTES")
                .evaluationType("LOWER_IS_BETTER")
                .targetThreshold(30.0)
                .warningThreshold(50.0)
                .criticalThreshold(75.0)
                .currentValue(82.0)
                .lastMeasuredAt(OffsetDateTime.now(ZoneOffset.UTC))
                .status("CRITICAL")
                .sourceProcess("Embarques")
                .startEvent("Inicio de carga")
                .endEvent("Cierre de embarque")
                .frequencyValue(10)
                .frequencyUnit("MINUTES")
                .isEnabled(true)
                .build());

        defaults.add(PerformanceKpiEntity.builder()
                .name("Puntualidad de transportistas")
                .description("Porcentaje de transportistas que llegan dentro de la ventana horaria programada.")
                .module("CARRIERS")
                .unit("PERCENTAGE")
                .evaluationType("HIGHER_IS_BETTER")
                .targetThreshold(95.0)
                .warningThreshold(85.0)
                .criticalThreshold(70.0)
                .currentValue(83.0)
                .lastMeasuredAt(OffsetDateTime.now(ZoneOffset.UTC))
                .status("WARNING")
                .sourceProcess("Control de citas")
                .startEvent("Hora programada de cita")
                .endEvent("Check-in real del transportista")
                .frequencyValue(1)
                .frequencyUnit("HOURS")
                .isEnabled(true)
                .build());

        kpiRepository.saveAll(defaults);
    }

    private KpiResponseDto mapToDto(PerformanceKpiEntity entity) {
        return KpiResponseDto.builder()
                .id(entity.getId())
                .name(entity.getName())
                .description(entity.getDescription())
                .module(entity.getModule())
                .unit(entity.getUnit())
                .evaluationType(entity.getEvaluationType())
                .thresholds(KpiThresholdsDto.builder()
                        .target(entity.getTargetThreshold())
                        .warning(entity.getWarningThreshold())
                        .critical(entity.getCriticalThreshold())
                        .rangeLow(entity.getRangeLow())
                        .rangeHigh(entity.getRangeHigh())
                        .build())
                .currentValue(entity.getCurrentValue())
                .lastMeasuredAt(entity.getLastMeasuredAt() != null ? entity.getLastMeasuredAt().toString() : null)
                .status(entity.getStatus())
                .sourceConfig(KpiSourceConfigDto.builder()
                        .sourceProcess(entity.getSourceProcess())
                        .startEvent(entity.getStartEvent())
                        .endEvent(entity.getEndEvent())
                        .frequencyValue(entity.getFrequencyValue())
                        .frequencyUnit(entity.getFrequencyUnit())
                        .active(entity.getIsEnabled())
                        .build())
                .isEnabled(entity.getIsEnabled())
                .createdAt(entity.getCreatedAt() != null ? entity.getCreatedAt().toString() : OffsetDateTime.now(ZoneOffset.UTC).toString())
                .updatedAt(entity.getUpdatedAt() != null ? entity.getUpdatedAt().toString() : OffsetDateTime.now(ZoneOffset.UTC).toString())
                .createdBy(entity.getCreatedByUser())
                .updatedBy(entity.getUpdatedByUser())
                .build();
    }
}
