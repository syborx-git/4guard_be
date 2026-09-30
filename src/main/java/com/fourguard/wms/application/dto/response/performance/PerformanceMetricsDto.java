package com.fourguard.wms.application.dto.response.performance;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Data Transfer Objects for Performance Analytics & Productivity Engine (HU-9 / HU-138 / HU-141 / HU-159).
 */
public class PerformanceMetricsDto {

    /**
     * Resumen Ejecutivo de 5 KPIs Principales (HU-141, HU-159).
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ExecutiveKpiResponse implements Serializable {
        private UUID branchId;
        private String branchName;
        private Double warehouseOccupancyPercentage; // Ocupación de almacén %
        private Double inventoryAccuracyPercentage;  // IRA % (Meta >= 99.5%)
        private Double onTimeDeliveryPercentage;     // OTIF % (Meta >= 98.0%)
        private Double avgDockToStockHours;          // Horas promedio Dock-to-Stock (Meta <= 2.0h)
        private Double avgOrderCycleHours;           // Horas promedio ciclo de orden (Meta <= 4.0h)
        private Long totalReceptionsToday;
        private Long totalOutboundsToday;
        private Long totalMovementsToday;
        private Long activeIncidencesCount;
        private OffsetDateTime lastCalculatedAt;
    }

    /**
     * Tiempos y Métricas de Procesos Inbound / Recepción (HU-138).
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class InboundProcessTimesResponse implements Serializable {
        private UUID branchId;
        private Long totalReceptions;
        private BigDecimal totalPiecesReceived;
        private Double avgUnloadMinutes; // Meta <= 45 min
        private Integer minUnloadMinutes;
        private Integer maxUnloadMinutes;
        private List<RampUsageMetric> rampMetrics;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RampUsageMetric implements Serializable {
        private UUID rampId;
        private String rampCode;
        private Long operationsCount;
        private Double avgStayMinutes;
        private String status;
    }

    /**
     * Productividad por Operador Montacarguista y Turno (HU-138, HU-161).
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class OperatorProductivityResponse implements Serializable {
        private UUID operatorId;
        private String operatorCode;
        private String fullName;
        private String jobTitle;
        private String licenseNumberDc3;
        private String licenseStatus;
        private UUID shiftId;
        private String shiftName;
        private Long receptionsHandled;
        private Long transfersCompleted;
        private Long outboundsDispatched;
        private Long totalMovements;
        private Double shiftEffectiveHours;
        private Double movementsPerHour; // Pallets or movements per hour (PPH)
        private Long targetMovements; // Meta de movimientos por turno
        private Double shiftCompliancePercentage; // % cumplimiento de meta de turno
        private String performanceBadge; // OPTIMAL, WARNING, CRITICAL
    }

    /**
     * Resumen de Productividad y Eficiencia por Turno de Trabajo (HU-140, HU-161).
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ShiftProductivitySummaryResponse implements Serializable {
        private UUID shiftId;
        private String shiftName;
        private String timeRange; // e.g. "06:00 - 14:00"
        private Integer activeOperatorsCount;
        private Long totalMovements;
        private Double avgMovementsPerHour;
        private Double targetPph;
        private Double compliancePercentage;
        private String status; // OPTIMAL, WARNING, CRITICAL
    }

    /**
     * Tiempos de los 4 Procesos Nodales ($T_0 \to T_1$).
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ProcessFlowTimesResponse implements Serializable {
        private String processName; // RECEPCION, ACOMODO, SURTIDO, EMBARQUE
        private String initialMilestone; // T0
        private String finalMilestone;   // T1
        private Double averageDurationMinutes;
        private Double targetStandardMinutes;
        private Double compliancePercentage;
        private String status; // OPTIMAL, WARNING, CRITICAL
    }

    /**
     * Definición y Reglas de un KPI (HU-138).
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class KpiThresholdsDto implements Serializable {
        private Double target;
        private Double warning;
        private Double critical;
        private Double rangeLow;
        private Double rangeHigh;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class KpiSourceConfigDto implements Serializable {
        private String sourceProcess;
        private String startEvent;
        private String endEvent;
        private Integer frequencyValue;
        private String frequencyUnit;
        private Boolean active;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class KpiResponseDto implements Serializable {
        private UUID id;
        private String name;
        private String description;
        private String module;
        private String unit;
        private String evaluationType;
        private KpiThresholdsDto thresholds;
        private Double currentValue;
        private String lastMeasuredAt;
        private String status;
        private KpiSourceConfigDto sourceConfig;
        private Boolean isEnabled;
        private String createdAt;
        private String updatedAt;
        private String createdBy;
        private String updatedBy;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CreateKpiRequestDto implements Serializable {
        private String name;
        private String description;
        private String module;
        private String unit;
        private String evaluationType;
        private KpiThresholdsDto thresholds;
        private KpiSourceConfigDto sourceConfig;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UpdateKpiRequestDto implements Serializable {
        private String name;
        private String description;
        private String module;
        private String unit;
        private String evaluationType;
        private KpiThresholdsDto thresholds;
        private KpiSourceConfigDto sourceConfig;
    }

    /**
     * Resumen del Circuito Delicado (10 Choferes & 7 Unidades de Transporte Propio) (Pablo Requirements).
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CircuitoDelicadoSummaryResponse implements Serializable {
        private Long totalTripsMonth;
        private Integer activeUnitsCount;
        private Integer activeDriversCount;
        private Double avgTurnaroundHours;
        private Long totalPalletsMoved;
        private BigDecimal totalPiecesMoved;
        private List<DriverPerformanceDetail> drivers;
        private List<VehiclePerformanceDetail> vehicles;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DriverPerformanceDetail implements Serializable {
        private UUID driverId;
        private String driverName;
        private String driverLicense;
        private String assignedVehiclePlates;
        private Long totalTripsMonth;
        private Long totalPalletsMoved;
        private BigDecimal totalPiecesMoved;
        private Double avgTurnaroundHours;
        private Integer boxRotationCount;
        private String status; // ACTIVO, EN_RUTA, DISPONIBLE, INACTIVO
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class VehiclePerformanceDetail implements Serializable {
        private UUID vehicleId;
        private String economicNumber;
        private String tractorPlates;
        private String transportType;
        private String assignedDriverName;
        private Long tripsCount;
        private Integer boxesTowedCount;
        private BigDecimal totalPieces;
        private Double operatingHours;
        private String status; // EN_RUTA, EN_PATIO, EN_MANTENIMIENTO
    }

    /**
     * Métricas de Ciclo Puerta a Puerta (Caseta QR -> Rampa -> Salida) (HU-151 -> HU-154).
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class GateToGateCycleResponse implements Serializable {
        private Double gateQrPreCheckinAvgMinutes;
        private Double gateToDockAvgMinutes;
        private Double dockOperationAvgMinutes;
        private Double dockToExitAvgMinutes;
        private Double totalGateToGateAvgMinutes;
        private Double targetGateToGateMinutes; // e.g. 120.0 min
        private Double compliancePercentage;
        private QualityLocksStatusDto qualityLocks;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class QualityLocksStatusDto implements Serializable {
        private Long f01ChecklistApprovedCount;
        private Long f01PendingCount;
        private Long weightValidationPassedCount;
        private Long weightValidationFailedCount;
        private Boolean allLocksEnforced;
    }

    /**
     * Evento cronológico de la Línea de Tiempo Auditada de Operadores (Timeline Audit por Folio / SSCC).
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MovementAuditTimelineDto implements Serializable {
        private UUID eventId;
        private OffsetDateTime timestamp;
        private UUID operatorId;
        private String operatorName;
        private String operatorCode;
        private String operationType; // DESCARGA_RAMPA, ACOMODO_PUTAWAY, SURTIDO_PICKING, VALIDACION_PESO, DESPACHO_EMBARQUE
        private String folio;
        private String sscc;
        private String sourceLocation;
        private String targetLocation;
        private Integer durationSeconds;
        private String qualityLockStatus; // APROBADO, PENDIENTE, N/A
        private String status; // COMPLETADO, EN_CURSO
        private String details;
    }

    /**
     * Respuesta para Encolamiento de Exportación de Reportes Excel (HU-158).
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ExportJobResponseDto implements Serializable {
        private String jobId;
        private String reportType;
        private String status; // QUEUED, PROCESSING, COMPLETED, FAILED
        private String downloadUrl;
        private String createdAt;
        private String message;
    }

    /**
     * Calibración de Metas Operativas del Usuario (Ajustar Metas Modal).
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class OperationalUserTargetsDto implements Serializable {
        private Double targetOccupancyPercentage;
        private Double targetIraPercentage;
        private Double targetOtifPercentage;
        private Double targetDockToStockHours;
        private Double targetOrderCycleHours;
        private Double targetForkliftPph;
        private Double targetInboundUnloadMinutes;
        private Double targetGateToGateMinutes;
        private String lastUpdatedBy;
        private OffsetDateTime lastUpdatedAt;
    }
}

