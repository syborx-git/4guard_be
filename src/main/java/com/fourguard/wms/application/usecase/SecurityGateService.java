package com.fourguard.wms.application.usecase;

import com.fourguard.wms.application.dto.request.outbound.CreateOutboundRequest;
import com.fourguard.wms.application.dto.request.reception.CreateCheckInRequest;
import com.fourguard.wms.application.dto.request.security.DriverCheckinSubmissionRequest;
import com.fourguard.wms.application.dto.request.security.GeneratePassRequest;
import com.fourguard.wms.application.dto.request.security.GuardCheckinCompletionRequest;
import com.fourguard.wms.application.dto.request.security.GuardCheckOutRequest;
import com.fourguard.wms.application.dto.response.outbound.OutboundResponse;
import com.fourguard.wms.application.dto.response.reception.ReceptionResponse;
import com.fourguard.wms.application.dto.response.security.PassResponse;
import com.fourguard.wms.application.dto.response.security.SecurityGatePublicCatalogsResponse;
import com.fourguard.wms.domain.exception.EntityNotFoundException;
import com.fourguard.wms.domain.exception.ValidationException;
import com.fourguard.wms.domain.enums.OutboundStatus;
import com.fourguard.wms.domain.enums.ReceptionStatus;
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
import java.time.temporal.ChronoUnit;
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
    private final WarehouseReceptionRepositoryPort warehouseReceptionRepositoryPort;
    private final WarehouseOutboundRepositoryPort warehouseOutboundRepositoryPort;
    private final WarehouseReceptionUseCase warehouseReceptionUseCase;
    private final WarehouseOutboundUseCase warehouseOutboundUseCase;
    private final SecurityAuditHelper securityAuditHelper;

    @Override
    @Transactional(readOnly = true)
    public SecurityGatePublicCatalogsResponse getPublicCatalogs(UUID organizationId) {
        log.info("Fetching public security gate catalogs for org: {}", organizationId);

        List<ClientEntity> clientEntities = organizationId != null
                ? clientRepositoryPort.findByOrganizationId(organizationId)
                : clientRepositoryPort.findAll();

        List<CarrierEntity> carrierEntities = organizationId != null
                ? carrierRepositoryPort.findByOrganizationId(organizationId)
                : carrierRepositoryPort.findAll();

        List<SecurityGatePublicCatalogsResponse.CatalogItemDto> clientDtos = (clientEntities != null ? clientEntities : Collections.<ClientEntity>emptyList()).stream()
                .filter(c -> c.getStatus() == null || "ACTIVE".equalsIgnoreCase(c.getStatus()) || "ACTIVO".equalsIgnoreCase(c.getStatus()))
                .map(c -> SecurityGatePublicCatalogsResponse.CatalogItemDto.builder()
                        .id(c.getId().toString())
                        .code(c.getExternalId() != null && !c.getExternalId().isBlank() ? c.getExternalId() : c.getId().toString())
                        .name(c.getName())
                        .tradeName(c.getName())
                        .build())
                .sorted(Comparator.comparing(SecurityGatePublicCatalogsResponse.CatalogItemDto::getName))
                .toList();

        List<SecurityGatePublicCatalogsResponse.CatalogItemDto> carrierDtos = (carrierEntities != null ? carrierEntities : Collections.<CarrierEntity>emptyList()).stream()
                .filter(c -> c.getStatus() == null || c.getStatus() == com.fourguard.wms.domain.enums.CarrierStatus.ACTIVE)
                .map(c -> SecurityGatePublicCatalogsResponse.CatalogItemDto.builder()
                        .id(c.getId().toString())
                        .code(c.getId().toString())
                        .name(c.getName())
                        .tradeName(c.getTradeName() != null && !c.getTradeName().isBlank() ? c.getTradeName() : c.getName())
                        .build())
                .sorted(Comparator.comparing(SecurityGatePublicCatalogsResponse.CatalogItemDto::getTradeName))
                .toList();

        List<String> transportTypes = List.of(
                "Caja Seca",
                "Caja Refrigerada",
                "Plataforma",
                "Tortón",
                "Rabón",
                "Camioneta 3.5",
                "Tráiler",
                "Contenedor",
                "Tolva",
                "Pipa",
                "Camioneta / Van",
                "Otro (Especificar)"
        );

        List<String> boxDimensions = List.of(
                "53 Pies",
                "48 Pies",
                "40 Pies",
                "20 Pies",
                "Tortón",
                "Rabón",
                "3.5 Toneladas",
                "N/A - Plataforma",
                "Otra Medida"
        );

        return SecurityGatePublicCatalogsResponse.builder()
                .clients(clientDtos)
                .carriers(carrierDtos)
                .transportTypes(transportTypes)
                .boxDimensions(boxDimensions)
                .build();
    }

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
            clientRepositoryPort.findByOrganizationId(entity.getOrganization().getId()).stream()
                    .filter(c -> (c.getExternalId() != null && c.getExternalId().equalsIgnoreCase(request.getClientCode())) ||
                                 (c.getId() != null && c.getId().toString().equalsIgnoreCase(request.getClientCode())) ||
                                 (c.getName() != null && c.getName().equalsIgnoreCase(request.getClientCode())))
                    .findFirst()
                    .ifPresent(entity::setClient);
        }
        if (request.getClientName() != null && !request.getClientName().isBlank()) {
            entity.setClientName(request.getClientName());
            if (entity.getClient() == null) {
                clientRepositoryPort.findByOrganizationId(entity.getOrganization().getId()).stream()
                        .filter(c -> c.getName() != null && c.getName().equalsIgnoreCase(request.getClientName()))
                        .findFirst()
                        .ifPresent(entity::setClient);
            }
        }
        if (request.getCarrierLineCode() != null && !request.getCarrierLineCode().isBlank()) {
            entity.setCarrierLineCode(request.getCarrierLineCode());
            carrierRepositoryPort.findByOrganizationId(entity.getOrganization().getId()).stream()
                    .filter(c -> (c.getTaxId() != null && c.getTaxId().equalsIgnoreCase(request.getCarrierLineCode())) ||
                                 (c.getId() != null && c.getId().toString().equalsIgnoreCase(request.getCarrierLineCode())) ||
                                 (c.getName() != null && c.getName().equalsIgnoreCase(request.getCarrierLineCode())))
                    .findFirst()
                    .ifPresent(entity::setCarrier);
        }
        if (request.getCarrierLine() != null && !request.getCarrierLine().isBlank()) {
            entity.setCarrierLine(request.getCarrierLine());
            if (entity.getCarrier() == null) {
                carrierRepositoryPort.findByOrganizationId(entity.getOrganization().getId()).stream()
                        .filter(c -> (c.getName() != null && c.getName().equalsIgnoreCase(request.getCarrierLine())) ||
                                     (c.getTradeName() != null && c.getTradeName().equalsIgnoreCase(request.getCarrierLine())))
                        .findFirst()
                        .ifPresent(entity::setCarrier);
            }
        }

        String docNo = request.getDocNumber();
        if (docNo == null || docNo.isBlank()) {
            docNo = "CARGA".equalsIgnoreCase(opType) ? request.getNoCartaPorte() : request.getRemision();
        }
        entity.setDocNumber(docNo);
        entity.setDocDate(request.getDocDate() != null ? request.getDocDate() : LocalDate.now());
        entity.setReceptionTime(request.getReceptionTime() != null ? request.getReceptionTime().truncatedTo(ChronoUnit.SECONDS) : LocalTime.now().truncatedTo(ChronoUnit.SECONDS));

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
        return list.stream().map(this::mapToResponseWithWarehouseStatus).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<PassResponse> getInYardPasses(UUID organizationId, UUID branchId) {
        List<SecurityPreCheckinEntity> list = preCheckinJpaRepository.findInYardPasses(
                organizationId, branchId
        );
        return list.stream().map(this::mapToResponseWithWarehouseStatus).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<PassResponse> getHistoryPasses(UUID organizationId, UUID branchId, String search) {
        List<SecurityPreCheckinEntity> list = preCheckinJpaRepository.findHistoryPasses(
                organizationId, branchId
        );
        if (search != null && !search.isBlank()) {
            String q = search.trim().toLowerCase();
            list = list.stream().filter(p ->
                    (p.getToken() != null && p.getToken().toLowerCase().contains(q)) ||
                    (p.getGeneratedFolio() != null && p.getGeneratedFolio().toLowerCase().contains(q)) ||
                    (p.getDriverName() != null && p.getDriverName().toLowerCase().contains(q)) ||
                    (p.getTractorPlates() != null && p.getTractorPlates().toLowerCase().contains(q)) ||
                    (p.getBoxPlates() != null && p.getBoxPlates().toLowerCase().contains(q)) ||
                    (p.getClientName() != null && p.getClientName().toLowerCase().contains(q)) ||
                    (p.getCarrierLine() != null && p.getCarrierLine().toLowerCase().contains(q))
            ).toList();
        }
        return list.stream().map(this::mapToResponseWithWarehouseStatus).toList();
    }

    @Override
    @Transactional
    public PassResponse completeCheckin(String token, GuardCheckinCompletionRequest request) {
        log.info("Guard completing check-in for token: {}, ramp: {}", token, request.getRampNumber());

        SecurityPreCheckinEntity entity = preCheckinJpaRepository.findByToken(token.trim().toUpperCase())
                .orElseThrow(() -> new EntityNotFoundException("Pase de acceso no encontrado: " + token));

        if ("COMPLETED".equalsIgnoreCase(entity.getStatus()) || "COMPLETED_EXIT".equalsIgnoreCase(entity.getStatus())) {
            throw new ValidationException("Este pase ya fue completado con el folio: " + entity.getGeneratedFolio());
        }

        String opType = (request.getOperationType() != null && !request.getOperationType().isBlank())
                ? request.getOperationType().toUpperCase().trim()
                : entity.getOperationType();

        // Update with guard-provided or overridden fields
        if (request.getClientCode() != null) entity.setClientCode(request.getClientCode());
        if (request.getClientName() != null) entity.setClientName(request.getClientName());
        if (entity.getClient() == null && entity.getClientCode() != null) {
            clientRepositoryPort.findByOrganizationId(entity.getOrganization().getId()).stream()
                    .filter(c -> (c.getExternalId() != null && c.getExternalId().equalsIgnoreCase(entity.getClientCode())) ||
                                 (c.getId() != null && c.getId().toString().equalsIgnoreCase(entity.getClientCode())) ||
                                 (c.getName() != null && c.getName().equalsIgnoreCase(entity.getClientCode())))
                    .findFirst()
                    .ifPresent(entity::setClient);
        }

        if (request.getCarrierLineCode() != null) entity.setCarrierLineCode(request.getCarrierLineCode());
        if (request.getCarrierLine() != null) entity.setCarrierLine(request.getCarrierLine());
        if (entity.getCarrier() == null && entity.getCarrierLineCode() != null) {
            carrierRepositoryPort.findByOrganizationId(entity.getOrganization().getId()).stream()
                    .filter(c -> (c.getTaxId() != null && c.getTaxId().equalsIgnoreCase(entity.getCarrierLineCode())) ||
                                 (c.getId() != null && c.getId().toString().equalsIgnoreCase(entity.getCarrierLineCode())) ||
                                 (c.getName() != null && c.getName().equalsIgnoreCase(entity.getCarrierLineCode())))
                    .findFirst()
                    .ifPresent(entity::setCarrier);
        }

        if (request.getDriverName() != null) entity.setDriverName(request.getDriverName());
        if (request.getTractorPlates() != null) entity.setTractorPlates(request.getTractorPlates().toUpperCase().trim());
        if (request.getBoxPlates() != null) entity.setBoxPlates(request.getBoxPlates().toUpperCase().trim());
        if (request.getNoEcoTractor() != null) entity.setNoEcoTractor(request.getNoEcoTractor());
        if (request.getTransportType() != null) entity.setTransportType(request.getTransportType());
        if (request.getBoxDimensions() != null) entity.setBoxDimensions(request.getBoxDimensions());
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
            inReq.setReceptionTime(entity.getReceptionTime() != null ? entity.getReceptionTime().truncatedTo(ChronoUnit.SECONDS) : LocalTime.now().truncatedTo(ChronoUnit.SECONDS));
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
        return mapToResponseWithWarehouseStatus(saved);
    }

    @Override
    @Transactional
    public PassResponse checkOut(String tokenOrFolio, GuardCheckOutRequest request) {
        log.info("Guard executing check-out for pass / folio: {}", tokenOrFolio);
        if (tokenOrFolio == null || tokenOrFolio.isBlank()) {
            throw new ValidationException("El token o folio es obligatorio para el check-out de caseta.");
        }

        String search = tokenOrFolio.trim().toUpperCase();
        SecurityPreCheckinEntity entity = preCheckinJpaRepository.findByToken(search)
                .or(() -> preCheckinJpaRepository.findByGeneratedFolio(tokenOrFolio.trim()))
                .orElseThrow(() -> new EntityNotFoundException("No se encontró el pase o folio de caseta: " + tokenOrFolio));

        LocalTime exitTime = request.getDepartureTime() != null ? request.getDepartureTime().truncatedTo(ChronoUnit.SECONDS) : LocalTime.now().truncatedTo(ChronoUnit.SECONDS);
        entity.setDepartureTime(exitTime);
        if (request.getExitObservations() != null) {
            entity.setExitObservations(request.getExitObservations());
        }
        if (request.getExitSealNumbers() != null && !request.getExitSealNumbers().isEmpty()) {
            entity.setExitSealNumbers(request.getExitSealNumbers());
        }
        if (request.getGuardNotes() != null && !request.getGuardNotes().isBlank()) {
            entity.setGuardNotes((entity.getGuardNotes() != null ? entity.getGuardNotes() + " | " : "") + request.getGuardNotes());
        }

        String currentUser = securityAuditHelper.getCurrentUsername() != null ? securityAuditHelper.getCurrentUsername() : "guardia";
        entity.setExitedBy(currentUser);
        entity.setExitedAt(OffsetDateTime.now());
        entity.setStatus("COMPLETED_EXIT");

        SecurityPreCheckinEntity saved = preCheckinJpaRepository.save(entity);
        return mapToResponseWithWarehouseStatus(saved);
    }

    @Override
    @Transactional
    public void cancelPass(UUID passId) {
        log.info("Cancelling / discarding pass with ID: {}", passId);
        SecurityPreCheckinEntity entity = preCheckinJpaRepository.findById(passId)
                .orElseThrow(() -> new EntityNotFoundException("Pase de acceso no encontrado con ID: " + passId));

        if ("COMPLETED".equalsIgnoreCase(entity.getStatus()) || "COMPLETED_EXIT".equalsIgnoreCase(entity.getStatus())) {
            throw new ValidationException("No se puede eliminar un pase que ya completó su autorización y registro en planta.");
        }

        preCheckinJpaRepository.delete(entity);
    }

    private PassResponse mapToResponse(SecurityPreCheckinEntity entity) {
        return mapToResponseWithWarehouseStatus(entity);
    }

    private PassResponse mapToResponseWithWarehouseStatus(SecurityPreCheckinEntity entity) {
        String whStatus = "REGISTRADO";
        boolean readyForExit = false;

        if (entity.getGeneratedFolio() != null && !entity.getGeneratedFolio().isBlank()) {
            String folio = entity.getGeneratedFolio().trim();
            if ("CARGA".equalsIgnoreCase(entity.getOperationType())) {
                Optional<WarehouseOutboundEntity> outbound = warehouseOutboundRepositoryPort.findByFolio(folio);
                if (outbound.isPresent()) {
                    WarehouseOutboundEntity out = outbound.get();
                    whStatus = out.getStatus() != null ? out.getStatus().name() : "REGISTERED";
                    readyForExit = (out.getStatus() == OutboundStatus.COMPLETED ||
                                    out.getStatus() == OutboundStatus.LOADED);
                }
            } else {
                Optional<WarehouseReceptionEntity> reception = warehouseReceptionRepositoryPort.findByFolio(folio);
                if (reception.isPresent()) {
                    WarehouseReceptionEntity rec = reception.get();
                    whStatus = rec.getStatus() != null ? rec.getStatus().name() : "REGISTERED";
                    readyForExit = (rec.getStatus() == ReceptionStatus.COMPLETED);
                }
            }
        }

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
                .departureTime(entity.getDepartureTime())
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
                .exitObservations(entity.getExitObservations())
                .exitSealNumbers(entity.getExitSealNumbers())
                .exitedBy(entity.getExitedBy())
                .exitedAt(entity.getExitedAt())
                .warehouseStatus(whStatus)
                .isReadyForExit(readyForExit)
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
