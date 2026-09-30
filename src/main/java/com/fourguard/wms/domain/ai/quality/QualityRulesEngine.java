package com.fourguard.wms.domain.ai.quality;

import com.fourguard.wms.application.dto.request.quality.ai.QualityAiRuleEvaluationRequest;
import com.fourguard.wms.application.dto.request.quality.ai.SamplingCalculationRequest;
import com.fourguard.wms.application.dto.response.quality.ai.QualityAiEvaluationResponse;
import com.fourguard.wms.application.dto.response.quality.ai.SamplingCalculationResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Deterministic in-memory Rules Engine for Quality Management (QM).
 * Evaluates official physical tolerances, pest rules, packaging conditions,
 * and sampling tables in &lt; 1 millisecond with zero external AI/LLM overhead.
 *
 * Grounded on:
 * - IT01-PO-GC-8.6-01 Rev. 01 (Condiciones de Aceptación y Rechazo)
 * - IT02-PO-GC-8.6-02 Rev. 01 (Acondicionamiento y Traspaleo de Material)
 * - PO-GC-8.6-03 Rev. 01 (Liberación de Carga)
 * - IT01-PO-GC-8.6-02 Rev. 01 (Condiciones de Almacenamiento y Humedad)
 * - IT01-PO-GC-8.6-04 Rev. 02 (Muestreo de Materiales y Café Verde)
 * - PO-GC-7.1.4-12 Rev. 01 (Manejo Integrado de Plagas)
 */
@Component
@Slf4j
public class QualityRulesEngine {

    // ─── 1. EVALUACIÓN DETERMINISTA DE REGLAS DE INSPECCIÓN FÍSICA ──────

    public QualityAiEvaluationResponse evaluatePhysicalInspection(QualityAiRuleEvaluationRequest req) {
        long startTime = System.currentTimeMillis();
        List<String> violatedRules = new ArrayList<>();
        List<String> appliedInstructions = new ArrayList<>();

        boolean requiresTraspaleo = false;
        boolean isDiurexAllowed = false;
        int recommendedPlayoWraps = 3;
        String verdict = "CONFORME";
        String severity = "OK";
        StringBuilder action = new StringBuilder("Material y transporte conformes con los estándares de calidad e inocuidad.");

        // Regla 1: Detección de plagas o indicios (Tolerancia CERO)
        if (Boolean.TRUE.equals(req.getIsPestDetected())) {
            verdict = "BLOQUEO_PNC";
            severity = "CRITICAL";
            violatedRules.add("IT01-PO-GC-8.6-01: Presencia de plagas detectada (0 tolerancia).");
            appliedInstructions.add("PO-GC-7.1.4-12");
            appliedInstructions.add("PO-GC-8.7-01");
            action = new StringBuilder("BLOQUEO INMEDIATO 'CRITICAL' (Rojo). Inmovilizar tarima en WMS, aislar zona y notificar a Dirección General y Cliente de inmediato.");
        }

        // Regla 2: Aromas químicos o combustibles (Tolerancia CERO)
        if (Boolean.TRUE.equals(req.getIsChemicalAromaDetected())) {
            verdict = "BLOQUEO_PNC";
            severity = "CRITICAL";
            violatedRules.add("IT01-PO-GC-8.6-01: Presencia de aroma químico, solvente o combustible (0 tolerancia).");
            action = new StringBuilder("Rechazar unidad de transporte o bloquear tarima por riesgo inminente de contaminación cruzada.");
        }

        // Regla 3: Inclinación de tarima / riesgo de colapso (> 5.0 grados)
        if (req.getPalletTiltDegrees() != null && req.getPalletTiltDegrees() > 5.0) {
            verdict = "ACONDICIONAMIENTO_REQUERIDO";
            if (!"CRITICAL".equals(severity)) severity = "WARNING";
            violatedRules.add(String.format(Locale.US, "IT01-PO-GC-8.6-01: Inclinación de %.1f° excede la tolerancia máxima de 5.0°.", req.getPalletTiltDegrees()));
            appliedInstructions.add("IT02-PO-GC-8.6-02");
            action.append(" Reacomodar estiba y re-emplayar con mínimo 3 vueltas en base, 70% altura y 3 en corona bajo IT02-PO-GC-8.6-02.");
        }

        // Regla 4: Daño en empaque primario (> 1 unidad = Rechazo/Bloqueo)
        if (req.getDamagedPrimaryUnits() != null && req.getDamagedPrimaryUnits() > 1) {
            verdict = "BLOQUEO_PNC";
            severity = "CRITICAL";
            violatedRules.add(String.format("IT01-PO-GC-8.6-01: %d unidades dañadas en empaque primario (Tolerancia máx: 1 unidad).", req.getDamagedPrimaryUnits()));
            appliedInstructions.add("IT02-PO-GC-8.6-02");
            action.append(" Retirar unidades dañadas con guantes anticorte en contenedor azul y levantar folio de Producto No Conforme.");
        }

        // Regla 5: Daño en empaque secundario (> 2 cajas = Rechazo)
        if (req.getDamagedSecondaryBoxes() != null && req.getDamagedSecondaryBoxes() > 2) {
            verdict = "BLOQUEO_PNC";
            if (!"CRITICAL".equals(severity)) severity = "WARNING";
            violatedRules.add(String.format("IT01-PO-GC-8.6-01: %d cajas dañadas en empaque secundario (Tolerancia máx: 2 cajas).", req.getDamagedSecondaryBoxes()));
            appliedInstructions.add("IT02-PO-GC-8.6-02");
            action.append(" Bloquear pallet para inspección y reacondicionamiento bajo IT02.");
        }

        // Regla 6: Regla de Diurex en Producto Terminado (ESTRICTAMENTE PROHIBIDO)
        if (Boolean.TRUE.equals(req.getIsUseDiurexRequested())) {
            boolean isGlassEmpty = req.getProductType() != null && req.getProductType().toUpperCase().contains("FRASCO");
            if (!isGlassEmpty) {
                isDiurexAllowed = false;
                verdict = "NO_CONFORME";
                if (!"CRITICAL".equals(severity)) severity = "WARNING";
                violatedRules.add("IT02-PO-GC-8.6-02 (2.4): Prohibido usar Diurex en Producto Terminado (daña cajas y etiquetas).");
                appliedInstructions.add("IT02-PO-GC-8.6-02");
                action.append(" PROHIBIDO EL USO DE DIUREX. Aplicar emplayado con film plástico.");
            } else {
                isDiurexAllowed = true;
            }
        }

        // Regla 7: Tablas o tacones rotos en tarima (1 reparable, >= 2 requiere traspaleo)
        if (req.getBrokenBoardsCount() != null) {
            if (req.getBrokenBoardsCount() >= 2) {
                requiresTraspaleo = true;
                verdict = "ACONDICIONAMIENTO_REQUERIDO";
                if (!"CRITICAL".equals(severity)) severity = "WARNING";
                violatedRules.add(String.format("IT02-PO-GC-8.6-02 (2.5): %d tablas/tacones rotos superan el límite de reparación in situ.", req.getBrokenBoardsCount()));
                appliedInstructions.add("IT02-PO-GC-8.6-02");
                action.append(" Ejecutar TRASPALEO COMPLETO a tarima nueva con escafandra, guantes y faja.");
            } else if (req.getBrokenBoardsCount() == 1) {
                appliedInstructions.add("IT02-PO-GC-8.6-02");
                action.append(" Reparar tarima in situ con apoyo del montacarguista colocando 1 tabla/tacón de repuesto.");
            }
        }

        // Regla 8: Vueltas de playo recomendadas
        if (req.getProductType() != null && req.getProductType().toUpperCase().contains("FRASCO")) {
            recommendedPlayoWraps = 5;
        } else {
            recommendedPlayoWraps = 3;
        }

        // Regla 9: Humedad relativa ambiental (> 65% HR)
        if (req.getRelativeHumidityPercent() != null && req.getRelativeHumidityPercent() > 65.0) {
            verdict = "ALERTA_AMBIENTAL";
            if (!"CRITICAL".equals(severity)) severity = "WARNING";
            violatedRules.add(String.format(Locale.US, "IT01-PO-GC-8.6-02: Humedad relativa de %.1f%% HR excede el límite crítico de 65%% HR.", req.getRelativeHumidityPercent()));
            appliedInstructions.add("IT01-PO-GC-8.6-02");
            action.append(" Reportar de inmediato en canal 'Incidencias de calidad' para encendido de deshumidificadores.");
        }

        // Regla 10: Rasgadura en lona de transporte (> 5.0 cm)
        if (req.getTarpTearLengthCm() != null && req.getTarpTearLengthCm() > 5.0) {
            verdict = "NO_CONFORME";
            if (!"CRITICAL".equals(severity)) severity = "WARNING";
            violatedRules.add(String.format(Locale.US, "IT01-PO-GC-8.6-01: Rasgadura en lona de %.1f cm supera la tolerancia máxima de 5.0 cm.", req.getTarpTearLengthCm()));
            action.append(" Rechazar unidad de transporte o solicitar cambio de vehículo.");
        }

        long latency = System.currentTimeMillis() - startTime;

        return QualityAiEvaluationResponse.builder()
                .verdict(verdict)
                .recommendedAction(action.toString().trim())
                .severity(severity)
                .violatedRules(violatedRules)
                .appliedInstructions(appliedInstructions)
                .requiresTraspaleo(requiresTraspaleo)
                .recommendedPlayoWraps(recommendedPlayoWraps)
                .isDiurexAllowed(isDiurexAllowed)
                .evaluationLatencyMs(latency)
                .build();
    }

    // ─── 2. CALCULADOR OFICIAL DE MUESTREO (IT01-PO-GC-8.6-04) ──────────

    public SamplingCalculationResponse calculateSampling(SamplingCalculationRequest req) {
        String type = req.getMaterialType() != null ? req.getMaterialType().toUpperCase().trim() : "DESCONOCIDO";

        switch (type) {
            case "ETIQUETAS":
            case "ETIQUETA_POSTETA":
                return SamplingCalculationResponse.builder()
                        .materialType("Etiquetas en Posteta")
                        .samplingStandardUnit("Posteta")
                        .requiredSampleQuantity(50.0)
                        .unitOfMeasure("Unidades")
                        .samplingMethod("Toma aleatoria en inicio, mitad y fin de descarga")
                        .requiredEquipment("Bolsas plásticas de oficina, etiqueta F02")
                        .destinationTransport("Calmo / Aguillón vía Seguridad Patrimonial")
                        .mandatoryInstructions(List.of("Inspección visual libre de manchas", "Colocar etiqueta F02-PO-GC-8.6-04", "Llenar remisión F03-PO-GC-8.6-04"))
                        .officialNormReference("IT01-PO-GC-8.6-04 Rev. 02 (Tabla 1)")
                        .build();

            case "ETIQUETAS_BOBINA":
            case "ETIQUETAS_CON_ADHERIBLE":
                return SamplingCalculationResponse.builder()
                        .materialType("Etiquetas con Adherible (Bobina)")
                        .samplingStandardUnit("Bobina")
                        .requiredSampleQuantity(6.0)
                        .unitOfMeasure("Metros Lineales")
                        .samplingMethod("Corte de muestra continuo en bobina")
                        .requiredEquipment("Cúter, guantes anticorte, etiqueta F02")
                        .destinationTransport("Seguridad Patrimonial (Transporte Autorizado)")
                        .mandatoryInstructions(List.of("Cerrar pallet/caja con playo tras extracción", "Etiquetar con SKU, Lote, Rampa y Muestreador"))
                        .officialNormReference("IT01-PO-GC-8.6-04 Rev. 02 (Tabla 1)")
                        .build();

            case "TAPAS_CULINARIOS":
            case "TAPAS_CAFES":
            case "TAPAS":
                return SamplingCalculationResponse.builder()
                        .materialType("Tapas (Culinarios / Cafés)")
                        .samplingStandardUnit("Cajas")
                        .requiredSampleQuantity(20.0)
                        .unitOfMeasure("Unidades")
                        .samplingMethod("Muestreo aleatorio de cajas distribuidas en la carga")
                        .requiredEquipment("Guantes de látex/nitrilo, bolsas limpias")
                        .destinationTransport("Seguridad Patrimonial")
                        .mandatoryInstructions(List.of("Verificar ausencia de deformaciones", "Identificar con etiqueta F02"))
                        .officialNormReference("IT01-PO-GC-8.6-04 Rev. 02 (Tabla 1)")
                        .build();

            case "CAFE_VERDE":
            case "CAFE":
                return SamplingCalculationResponse.builder()
                        .materialType("Café Verde (Costales de Yute)")
                        .samplingStandardUnit("Sacos / Tarimas")
                        .requiredSampleQuantity(3.0)
                        .unitOfMeasure("Kilogramos (3.0 kg)")
                        .samplingMethod("Calador en diagonal a 1-2 sacos por tarima (~150g por saco)")
                        .requiredEquipment("Calador manual, bolsa de muestra, balanza, etiqueta F02")
                        .destinationTransport("Transportes Aguillón / Calmo")
                        .mandatoryInstructions(List.of(
                                "Insertar calador en diagonal (parte negra sin punta al interior)",
                                "Extraer ~150g por saco hasta completar 3.0 kg",
                                "Cerrar bolsa con aire y agitar 30 segundos obligatorios para homogeneizar",
                                "Llenar nota de remisión F03-PO-GC-8.6-04"
                        ))
                        .officialNormReference("IT01-PO-GC-8.6-04 Rev. 02 (Tabla 2)")
                        .build();

            case "GARRAFAS":
                return SamplingCalculationResponse.builder()
                        .materialType("Garrafas")
                        .samplingStandardUnit("Pieza / Pallet")
                        .requiredSampleQuantity(4.0)
                        .unitOfMeasure("Unidades")
                        .samplingMethod("Extracción de 4 unidades representativas por pallet")
                        .requiredEquipment("Guantes limpios, etiqueta F02")
                        .destinationTransport("Seguridad Patrimonial")
                        .mandatoryInstructions(List.of("Re-emplayar pallet tras retiro"))
                        .officialNormReference("IT01-PO-GC-8.6-04 Rev. 02")
                        .build();

            case "EXHIBIDORES":
            case "ESTUCHES":
            case "CHAROLAS":
                return SamplingCalculationResponse.builder()
                        .materialType("Exhibidores / Estuches / Charolas")
                        .samplingStandardUnit("Corrugado / Paquete")
                        .requiredSampleQuantity(30.0)
                        .unitOfMeasure("Unidades")
                        .samplingMethod("Toma aleatoria de 30 unidades por lote")
                        .requiredEquipment("Bolsas limpias, formato F02")
                        .destinationTransport("Seguridad Patrimonial")
                        .mandatoryInstructions(List.of("Verificar empaque sin dobleces no especificados"))
                        .officialNormReference("IT01-PO-GC-8.6-04 Rev. 02")
                        .build();

            case "CAJAS":
            case "CORRUGADO":
                return SamplingCalculationResponse.builder()
                        .materialType("Cajas de Cartón Corrugado")
                        .samplingStandardUnit("Corrugado / Paquete")
                        .requiredSampleQuantity(10.0)
                        .unitOfMeasure("Unidades")
                        .samplingMethod("Extracción de 10 cajas representativas por lote")
                        .requiredEquipment("Etiqueta F02, formato F03")
                        .destinationTransport("Seguridad Patrimonial")
                        .mandatoryInstructions(List.of("Inspección de resistencia y pegado"))
                        .officialNormReference("IT01-PO-GC-8.6-04 Rev. 02")
                        .build();

            default:
                return SamplingCalculationResponse.builder()
                        .materialType(req.getMaterialType())
                        .samplingStandardUnit("Genérico")
                        .requiredSampleQuantity(10.0)
                        .unitOfMeasure("Unidades")
                        .samplingMethod("Muestreo aleatorio representativo inicio/mitad/fin")
                        .requiredEquipment("Bolsa estéril, guantes, etiqueta F02")
                        .destinationTransport("Seguridad Patrimonial")
                        .mandatoryInstructions(List.of("Consultar con el Gerente de Calidad si aplica protocolo especial"))
                        .officialNormReference("IT01-PO-GC-8.6-04 Rev. 02")
                        .build();
        }
    }
}
