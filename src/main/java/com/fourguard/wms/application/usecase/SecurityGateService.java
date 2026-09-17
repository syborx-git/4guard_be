package com.fourguard.wms.application.usecase;

import com.fourguard.wms.application.dto.request.outbound.CreateOutboundRequest;
import com.fourguard.wms.application.dto.request.reception.CreateCheckInRequest;
import com.fourguard.wms.application.dto.request.security.DriverCheckinSubmissionRequest;
import com.fourguard.wms.application.dto.request.security.GeneratePassRequest;
import com.fourguard.wms.application.dto.request.security.GuardCheckinCompletionRequest;
import com.fourguard.wms.application.dto.response.outbound.OutboundResponse;
import com.fourguard.wms.application.dto.response.reception.ReceptionResponse;
import com.fourguard.wms.application.dto.response.security.PassResponse;
import com.fourguard.wms.domain.exception.EntityNotFoundException;
import com.fourguard.wms.domain.exception.ValidationException;
import com.fourguard.wms.domain.ports.in.SecurityGateUseCase;
import com.fourguard.wms.domain.ports.in.WarehouseOutboundUseCase;
import com.fourguard.wms.domain.ports.in.WarehouseReceptionUseCase;
import com.fourguard.wms.domain.ports.out.*;
import com.fourguard.wms.infrastructure.persistence.entity.*;
import com.fourguard.wms.infrastructure.persistence.repository.SecurityPreCheckinJpaRepository;
import com.fourguard.wms.shared.audit.SecurityAuditHelper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class SecurityGateService implements SecurityGateUseCase {

    private final SecurityPreCheckinJpaRepository preCheckinJpaRepository;
    private final OrganizationRepositoryPort organizationRepositoryPort;
    private final BranchRepositoryPort branchRepositoryPort;
    private final ClientRepositoryPort clientRepositoryPort;
    private final CarrierRepositoryPort carrierRepositoryPort;
    private final LocationRepositoryPort locationRepositoryPort;
    private final ForkliftOperatorRepositoryPort forkliftOperatorRepositoryPort;
    private final WarehouseReceptionUseCase warehouseReceptionUseCase;
    private final WarehouseOutboundUseCase warehouseOutboundUseCase;
    private final SecurityAuditHelper securityAuditHelper;

    @Override
    @Transactional
    public PassResponse generatePass(GeneratePassRequest request) {
        log.info("Generating digital pass QR for organization: {}, branch: {}", request.getOrganizationId(), request.getBranchId());

        OrganizationEntity organization = organizationRepositoryPort.findById(request.getOrganizationId())
                .orElseThrow(() -> new EntityNotFoundException("Organización no encontrada: " + request.getOrganizationId()));

        BranchEntity branch = branchRepositoryPort.findById(request.getBranchId())
                .orElseThrow(() -> new EntityNotFoundException("Sucursal no encontrada: " + request.getBranchId()));

        String token = generateUniqueToken();

        ClientEntity client = null;
        if (request.getClientCode() != null && !request.getClientCode().isBlank()) {
            client = clientRepositoryPort.findByOrganizationId(organization.getId()).stream()
                    .filter(c -> (c.getExternalId() != null && c.getExternalId().equalsIgnoreCase(request.getClientCode())) ||
                                 (c.getName() != null && c.getName().equalsIgnoreCase(request.getClientCode())))
                    .findFirst()
                    .orElse(null);
        }

        CarrierEntity carrier = null;
        if (request.getCarrierLineCode() != null && !request.getCarrierLineCode().isBlank()) {
            carrier = carrierRepositoryPort.findByOrganizationId(organization.getId()).stream()
                    .filter(c -> (c.getTaxId() != null && c.getTaxId().equalsIgnoreCase(request.getCarrierLineCode())) ||
                                 (c.getName() != null && c.getName().equalsIgnoreCase(request.getCarrierLineCode())))
                    .findFirst()
                    .orElse(null);
        }

        String opType = (request.getOperationType() != null && !request.getOperationType().isBlank())
                ? request.getOperationType().toUpperCase().trim()
                : "DESCARGA";

        SecurityPreCheckinEntity entity = SecurityPreCheckinEntity.builder()
                .token(token)
                .organization(organization)
                .branch(branch)
                .status("PENDING_DRIVER")
                .operationType(opType)
                .client(client)
                .clientCode(request.getClientCode())
                .clientName(request.getClientName() != null ? request.getClientName() : (client != null ? client.getName() : null))
                .carrier(carrier)
                .carrierLineCode(request.getCarrierLineCode())
                .carrierLine(request.getCarrierLine() != null ? request.getCarrierLine() : (carrier != null ? carrier.getName() : null))
                .driverName(request.getDriverName())
                .tractorPlates(request.getTractorPlates())
                .docNumber(request.getDocNumber())
                .expiresAt(OffsetDateTime.now().plusHours(24))
                .build();

        SecurityPreCheckinEntity saved = preCheckinJpaRepository.save(entity);
        return mapToResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public PassResponse getPassByToken(String token) {
        if (token == null || token.isBlank()) {
            throw new ValidationException("El token de acceso es obligatorio");
        }

        SecurityPreCheckinEntity entity = preCheckinJpaRepository.findByToken(token.trim().toUpperCase())
                .orElseThrow(() -> new EntityNotFoundException("El pase de acceso no existe o ya no es válido: " + token));

        if (entity.getExpiresAt().isBefore(OffsetDateTime.now())) {
            throw new ValidationException("El pase de acceso ha expirado. Solicite un nuevo código QR en Caseta.");
        }

        return mapToResponse(entity);
    }

    @Override
    @Transactional
    public PassResponse submitDriverCheckin(String token, DriverCheckinSubmissionRequest request) {
        log.info("Driver self-registration submission for pass token: {}", token);

        SecurityPreCheckinEntity entity = preCheckinJpaRepository.findByToken(token.trim().toUpperCase())
                .orElseThrow(() -> new EntityNotFoundException("Pase de acceso no encontrado: " + token));

        if ("COMPLETED".equalsIgnoreCase(entity.getStatus())) {
            throw new ValidationException("Este pase ya fue procesado y autorizado por Caseta de Seguridad.");
        }
        if (entity.getExpiresAt().isBefore(OffsetDateTime.now())) {
            throw new ValidationException("El pase de acceso ha expirado.");
        }

        String opType = (request.getOperationType() != null && !request.getOperationType().isBlank())
                ? request.getOperationType().toUpperCase().trim()
                : entity.getOperationType();

        entity.setOperationType(opType);
        entity.setDriverName(request.getDriverName());
        entity.setDriverLicense(request.getDriverLicense());
        entity.setTractorPlates(request.getTractorPlates() != null ? request.getTractorPlates().toUpperCase().trim() : null);
        entity.setNoEcoTractor(request.getNoEcoTractor());
        entity.setBoxPlates(request.getBoxPlates() != null ? request.getBoxPlates().toUpperCase().trim() : null);
        entity.setBoxDimensions(request.getBoxDimensions());
        entity.setTransportType(request.getTransportType());

        if (request.getClientCode() != null && !request.getClientCode().isBlank()) {
            entity.setClientCode(request.getClientCode());
        }
        if (request.getClientName() != null && !request.getClientName().isBlank()) {
            entity.setClientName(request.getClientName());
        }
        if (request.getCarrierLineCode() != null && !request.getCarrierLineCode().isBlank()) {
            entity.setCarrierLineCode(request.getCarrierLineCode());
        }
        if (request.getCarrierLine() != null && !request.getCarrierLine().isBlank()) {
            entity.setCarrierLine(request.getCarrierLine());
        }

        String docNo = request.getDocNumber();
        if (docNo == null || docNo.isBlank()) {
            docNo = "CARGA".equalsIgnoreCase(opType) ? request.getNoCartaPorte() : request.getRemision();
        }
        entity.setDocNumber(docNo);
        entity.setDocDate(request.getDocDate() != null ? request.getDocDate() : LocalDate.now());
        entity.setReceptionTime(request.getReceptionTime() != null ? request.getReceptionTime() : LocalTime.now());

        if (request.getSealNumbers() != null && !request.getSealNumbers().isEmpty()) {
            List<String> upperSeals = new ArrayList<>();
            for (String s : request.getSealNumbers()) {
                if (s != null && !s.isBlank()) upperSeals.add(s.toUpperCase().trim());
            }
            entity.setSealNumbers(upperSeals);
        }

        entity.setChecklistData(request.getChecklistData());
        entity.setObservations(request.getObservations());
        entity.setDriverSignature(request.getDriverSignature());
        entity.setDriverSignedAt(OffsetDateTime.now());
        entity.setStatus("SUBMITTED");

        SecurityPreCheckinEntity saved = preCheckinJpaRepository.save(entity);
        return mapToResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public List<PassResponse> getActivePasses(UUID organizationId, UUID branchId) {
        List<SecurityPreCheckinEntity> list = preCheckinJpaRepository.findActivePasses(
                organizationId, branchId, OffsetDateTime.now()
        );
        return list.stream().map(this::mapToResponse).toList();
    }

    @Override
    @Transactional
    public PassResponse completeCheckin(String token, GuardCheckinCompletionRequest request) {
        log.info("Guard completing check-in for token: {}, ramp: {}", token, request.getRampNumber());

        SecurityPreCheckinEntity entity = preCheckinJpaRepository.findByToken(token.trim().toUpperCase())
                .orElseThrow(() -> new EntityNotFoundException("Pase de acceso no encontrado: " + token));

        if ("COMPLETED".equalsIgnoreCase(entity.getStatus())) {
            throw new ValidationException("Este pase ya fue completado con el folio: " + entity.getGeneratedFolio());
        }

        String opType = (request.getOperationType() != null && !request.getOperationType().isBlank())
                ? request.getOperationType().toUpperCase().trim()
                : entity.getOperationType();

        // Update with guard-provided or overridden fields
        if (request.getClientCode() != null) entity.setClientCode(request.getClientCode());
        if (request.getClientName() != null) entity.setClientName(request.getClientName());
        if (request.getCarrierLineCode() != null) entity.setCarrierLineCode(request.getCarrierLineCode());
        if (request.getCarrierLine() != null) entity.setCarrierLine(request.getCarrierLine());
        if (request.getDriverName() != null) entity.setDriverName(request.getDriverName());
        if (request.getTractorPlates() != null) entity.setTractorPlates(request.getTractorPlates().toUpperCase().trim());
        if (request.getBoxPlates() != null) entity.setBoxPlates(request.getBoxPlates().toUpperCase().trim());
        if (request.getNoEcoTractor() != null) entity.setNoEcoTractor(request.getNoEcoTractor());
        if (request.getTransportType() != null) entity.setTransportType(request.getTransportType());
        if (request.getDocNumber() != null) entity.setDocNumber(request.getDocNumber());
        if (request.getDocDate() != null) entity.setDocDate(request.getDocDate());
        if (request.getReceptionTime() != null) entity.setReceptionTime(request.getReceptionTime());
        if (request.getSealNumbers() != null && !request.getSealNumbers().isEmpty()) {
            entity.setSealNumbers(request.getSealNumbers());
        }
        if (request.getObservations() != null) entity.setObservations(request.getObservations());
        entity.setGuardNotes(request.getGuardNotes());
        entity.setRampNumber(request.getRampNumber());
        entity.setRampCode(request.getRampCode());

        String generatedFolio = null;

        List<String> finalSeals = (entity.getSealNumbers() != null && !entity.getSealNumbers().isEmpty())
                ? entity.getSealNumbers()
                : List.of("SEAL-4G-001");

        if ("CARGA".equalsIgnoreCase(opType)) {
            // Register Outbound in wms.warehouse_outbounds (Folio SAL-YYYY-XXXXXX)
            CreateOutboundRequest outReq = new CreateOutboundRequest();
            outReq.setOrganizationId(entity.getOrganization().getId());
            outReq.setBranchId(entity.getBranch().getId());
            outReq.setClientId(entity.getClient() != null ? entity.getClient().getId() : request.getClientId());
            outReq.setClientCode(entity.getClientCode());
            outReq.setClientName(entity.getClientName());
            outReq.setCarrierId(entity.getCarrier() != null ? entity.getCarrier().getId() : request.getCarrierId());
            outReq.setCarrierName(entity.getCarrierLine());
            outReq.setCarrierLineCode(entity.getCarrierLineCode());
            outReq.setCarrierLine(entity.getCarrierLine());
            outReq.setRampId(request.getRampId());
            outReq.setRampNumber(request.getRampNumber());
            outReq.setRampCode(request.getRampCode());
            outReq.setForkliftOperatorId(request.getForkliftOperatorId());
            outReq.setForkliftOperatorName(request.getForkliftOperatorName());
            outReq.setTransportType(entity.getTransportType() != null ? entity.getTransportType() : "TRAILER");
            outReq.setDriverName(entity.getDriverName() != null ? entity.getDriverName() : "Operador Transportista");
            outReq.setTractorPlates(entity.getTractorPlates() != null ? entity.getTractorPlates() : "S/P");
            outReq.setBoxPlates(entity.getBoxPlates() != null ? entity.getBoxPlates() : "S/P");
            outReq.setEconomicNumber(entity.getNoEcoTractor());
            outReq.setBoxEconomicNumber(entity.getBoxEconomicNumber());
            outReq.setSealNumber(String.join(", ", finalSeals));
            outReq.setRemisionNo(entity.getDocNumber() != null ? entity.getDocNumber() : "CP-" + entity.getToken());
            outReq.setObservations(entity.getObservations());
            outReq.setStatus("REGISTERED");
            outReq.setSelectedItemIds(Collections.emptyList());

            OutboundResponse outRes = warehouseOutboundUseCase.createOutbound(outReq);
            generatedFolio = outRes.getFolio();
        } else {
            // Register Inbound in wms.warehouse_receptions (Folio REC-YYYY-XXXXXX)
            CreateCheckInRequest inReq = new CreateCheckInRequest();
            inReq.setOrganizationId(entity.getOrganization().getId());
            inReq.setBranchId(entity.getBranch().getId());
            inReq.setClientId(entity.getClient() != null ? entity.getClient().getId() : request.getClientId());
            inReq.setClientCode(entity.getClientCode());
            inReq.setClientName(entity.getClientName());
            inReq.setCarrierId(entity.getCarrier() != null ? entity.getCarrier().getId() : request.getCarrierId());
            inReq.setCarrierLineCode(entity.getCarrierLineCode());
            inReq.setCarrierLine(entity.getCarrierLine());
            inReq.setRampId(request.getRampId());
            inReq.setRampNumber(request.getRampNumber());
            inReq.setRampCode(request.getRampCode());
            inReq.setForkliftOperatorId(request.getForkliftOperatorId());
            inReq.setDocNumber(entity.getDocNumber() != null ? entity.getDocNumber() : "REM-" + entity.getToken());
            inReq.setDocDate(entity.getDocDate() != null ? entity.getDocDate() : LocalDate.now());
            inReq.setReceptionTime(entity.getReceptionTime() != null ? entity.getReceptionTime() : LocalTime.now());
            inReq.setDriverName(entity.getDriverName() != null ? entity.getDriverName() : "Operador Transportista");
            inReq.setTractorPlates(entity.getTractorPlates() != null ? entity.getTractorPlates() : "S/P");
            inReq.setBoxPlates(entity.getBoxPlates() != null ? entity.getBoxPlates() : "S/P");
            inReq.setSealNumbers(finalSeals);
            inReq.setObservations(entity.getObservations());
            inReq.setTransportType(entity.getTransportType());
            inReq.setNoEcoTractor(entity.getNoEcoTractor());

            ReceptionResponse inRes = warehouseReceptionUseCase.createCheckIn(inReq);
            generatedFolio = inRes.getFolio();
        }

        entity.setStatus("COMPLETED");
        entity.setGeneratedFolio(generatedFolio);
        entity.setProcessedAt(OffsetDateTime.now());
        entity.setProcessedBy(securityAuditHelper.getCurrentUsername() != null ? securityAuditHelper.getCurrentUsername() : "guardia");

        SecurityPreCheckinEntity saved = preCheckinJpaRepository.save(entity);
        return mapToResponse(saved);
    }

    private PassResponse mapToResponse(SecurityPreCheckinEntity entity) {
        return PassResponse.builder()
                .id(entity.getId())
                .token(entity.getToken())
                .organizationId(entity.getOrganization().getId())
                .branchId(entity.getBranch().getId())
                .status(entity.getStatus())
                .operationType(entity.getOperationType())
                .clientId(entity.getClient() != null ? entity.getClient().getId() : null)
                .clientCode(entity.getClientCode())
                .clientName(entity.getClientName())
                .carrierId(entity.getCarrier() != null ? entity.getCarrier().getId() : null)
                .carrierLineCode(entity.getCarrierLineCode())
                .carrierLine(entity.getCarrierLine())
                .driverName(entity.getDriverName())
                .driverLicense(entity.getDriverLicense())
                .transportType(entity.getTransportType())
                .economicNumber(entity.getEconomicNumber() != null ? entity.getEconomicNumber() : entity.getNoEcoTractor())
                .boxEconomicNumber(entity.getBoxEconomicNumber())
                .tractorPlates(entity.getTractorPlates())
                .boxPlates(entity.getBoxPlates())
                .boxDimensions(entity.getBoxDimensions())
                .sealNumbers(entity.getSealNumbers())
                .docNumber(entity.getDocNumber())
                .docDate(entity.getDocDate())
                .receptionTime(entity.getReceptionTime())
                .checklistData(entity.getChecklistData())
                .observations(entity.getObservations())
                .driverSignature(entity.getDriverSignature())
                .driverSignedAt(entity.getDriverSignedAt())
                .rampId(entity.getRamp() != null ? entity.getRamp().getId() : null)
                .rampNumber(entity.getRampNumber())
                .rampCode(entity.getRampCode())
                .generatedFolio(entity.getGeneratedFolio())
                .processedBy(entity.getProcessedBy())
                .processedAt(entity.getProcessedAt())
                .expiresAt(entity.getExpiresAt())
                .createdAt(entity.getCreatedAt())
                .build();
    }

    private String generateUniqueToken() {
        String datePart = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String randomPart = UUID.randomUUID().toString().substring(0, 6).toUpperCase();
        return String.format("PASS-%s-%s", datePart, randomPart);
    }
}
