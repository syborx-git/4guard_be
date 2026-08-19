package com.fourguard.wms.application.usecase;

import com.fourguard.wms.application.dto.request.CreateForkliftOperatorRequest;
import com.fourguard.wms.application.dto.request.UpdateForkliftOperatorRequest;
import com.fourguard.wms.application.dto.request.UpdateForkliftOperatorStatusRequest;
import com.fourguard.wms.application.dto.response.ForkliftOperatorResponse;
import com.fourguard.wms.application.dto.response.audit.ForkliftOperatorAuditResponse;
import com.fourguard.wms.application.mapper.ForkliftOperatorMapper;
import com.fourguard.wms.domain.enums.ForkliftOperatorStatus;
import com.fourguard.wms.domain.enums.LicenseStatus;
import com.fourguard.wms.domain.exception.EntityNotFoundException;
import com.fourguard.wms.domain.exception.ValidationException;
import com.fourguard.wms.domain.ports.in.ForkliftOperatorUseCase;
import com.fourguard.wms.domain.ports.out.AuditLogRepositoryPort;
import com.fourguard.wms.domain.ports.out.ForkliftOperatorRepositoryPort;
import com.fourguard.wms.domain.ports.out.OrganizationRepositoryPort;
import com.fourguard.wms.domain.ports.out.UserRepositoryPort;
import com.fourguard.wms.infrastructure.persistence.entity.AuditLogEntity;
import com.fourguard.wms.infrastructure.persistence.entity.BranchEntity;
import com.fourguard.wms.infrastructure.persistence.entity.ForkliftOperatorEntity;
import com.fourguard.wms.infrastructure.persistence.entity.OrganizationEntity;
import com.fourguard.wms.infrastructure.persistence.entity.ShiftEntity;
import com.fourguard.wms.infrastructure.persistence.entity.UserEntity;
import com.fourguard.wms.infrastructure.persistence.repository.BranchJpaRepository;
import com.fourguard.wms.infrastructure.persistence.repository.ShiftJpaRepository;
import com.fourguard.wms.shared.audit.AuditService;
import com.fourguard.wms.shared.audit.SecurityAuditHelper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Application service implementing {@link ForkliftOperatorUseCase} (HU-142).
 *
 * <p>Business rules enforced:
 * <ul>
 *   <li>Code (MC-XXX) is auto-generated per organization and is immutable.</li>
 *   <li>{@code fullName} is always derived from firstName + lastNamePaternal + lastNameMaternal.</li>
 *   <li>{@code licenseStatus} is always recomputed from {@code licenseExpirationDate}.</li>
 *   <li>DC-3 license number must be unique within the organization.</li>
 *   <li>Every write operation is recorded in {@code wms.audit_logs} with field-level deltas.</li>
 * </ul>
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ForkliftOperatorService implements ForkliftOperatorUseCase {

    private static final String ENTITY_TYPE = "FORKLIFT_OPERATOR";

    private final ForkliftOperatorRepositoryPort operatorRepositoryPort;
    private final OrganizationRepositoryPort     organizationRepositoryPort;
    private final UserRepositoryPort             userRepositoryPort;
    private final AuditLogRepositoryPort         auditLogRepositoryPort;
    private final ShiftJpaRepository             shiftJpaRepository;
    private final BranchJpaRepository            branchJpaRepository;
    private final ForkliftOperatorMapper         mapper;
    private final SecurityAuditHelper            securityAuditHelper;
    private final AuditService                   auditService;

    // ─── CREATE ─────────────────────────────────────────────────────────────────

    @Override
    @Transactional
    public ForkliftOperatorResponse createOperator(CreateForkliftOperatorRequest request) {
        log.info("Creating forklift operator for organization: {}", request.getOrganizationId());

        OrganizationEntity organization = organizationRepositoryPort.findById(request.getOrganizationId())
                .orElseThrow(() -> new EntityNotFoundException(
                        "Organización no encontrada con ID: " + request.getOrganizationId()));

        // Validate DC-3 uniqueness
        if (operatorRepositoryPort.existsByOrganizationIdAndLicenseNumberDc3(
                request.getOrganizationId(), request.getLicenseNumberDc3())) {
            throw new ValidationException(
                    "Ya existe un montacarguista con la licencia DC-3 '" + request.getLicenseNumberDc3()
                    + "' en esta organización.");
        }

        ForkliftOperatorEntity entity = mapper.toEntity(request);
        entity.setOrganization(organization);

        // Auto-generate code: find the next available unique code (MC-001, MC-002, etc.)
        int seq = 1;
        String generatedCode;
        do {
            generatedCode = String.format("MC-%03d", seq++);
        } while (operatorRepositoryPort.existsByOrganizationIdAndCode(request.getOrganizationId(), generatedCode));
        entity.setCode(generatedCode);

        // Computed fields
        entity.setFullName(buildFullName(request.getFirstName(), request.getLastNamePaternal(), request.getLastNameMaternal()));
        entity.setLicenseStatus(computeLicenseStatus(request.getLicenseExpirationDate()));
        entity.setStatus(ForkliftOperatorStatus.ACTIVO);

        // Optional associations
        resolveShift(request.getShiftId(), entity);
        resolveBranch(request.getBranchId(), entity);

        String currentUser = securityAuditHelper.getCurrentUsername();
        entity.setCreatedBy(currentUser);
        entity.setUpdatedBy(currentUser);

        ForkliftOperatorEntity saved = operatorRepositoryPort.save(entity);

        logAudit(currentUser, "FORKLIFT_OPERATOR_CREATED", saved.getId(), null, saved);

        log.info("Forklift operator created: {} ({})", saved.getFullName(), saved.getCode());
        return mapper.toResponse(saved);
    }

    // ─── UPDATE ─────────────────────────────────────────────────────────────────

    @Override
    @Transactional
    public ForkliftOperatorResponse updateOperator(UpdateForkliftOperatorRequest request) {
        log.info("Updating forklift operator: {}", request.getId());

        ForkliftOperatorEntity existing = operatorRepositoryPort.findActiveById(request.getId())
                .orElseThrow(() -> new EntityNotFoundException(
                        "Montacarguista no encontrado con ID: " + request.getId()));

        // DC-3 uniqueness (excluding self)
        if (!existing.getLicenseNumberDc3().equalsIgnoreCase(request.getLicenseNumberDc3())) {
            if (operatorRepositoryPort.existsByOrganizationIdAndLicenseNumberDc3AndIdNot(
                    request.getOrganizationId(), request.getLicenseNumberDc3(), request.getId())) {
                throw new ValidationException(
                        "Ya existe otro montacarguista con la licencia DC-3 '" + request.getLicenseNumberDc3()
                        + "' en esta organización.");
            }
        }

        ForkliftOperatorEntity snapshot = cloneEntity(existing);

        mapper.updateEntityFromDto(request, existing);

        // Recompute derived fields
        existing.setFullName(buildFullName(request.getFirstName(), request.getLastNamePaternal(), request.getLastNameMaternal()));
        existing.setLicenseStatus(computeLicenseStatus(request.getLicenseExpirationDate()));

        // Optional associations
        resolveShift(request.getShiftId(), existing);
        resolveBranch(request.getBranchId(), existing);

        String currentUser = securityAuditHelper.getCurrentUsername();
        existing.setUpdatedBy(currentUser);

        ForkliftOperatorEntity saved = operatorRepositoryPort.save(existing);

        logAudit(currentUser, "FORKLIFT_OPERATOR_UPDATED", saved.getId(), snapshot, saved);

        return mapper.toResponse(saved);
    }

    // ─── READ ────────────────────────────────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public ForkliftOperatorResponse getOperatorById(UUID id) {
        log.debug("Fetching forklift operator: {}", id);
        return operatorRepositoryPort.findActiveById(id)
                .map(mapper::toResponse)
                .orElseThrow(() -> new EntityNotFoundException(
                        "Montacarguista no encontrado con ID: " + id));
    }

    @Override
    @Transactional(readOnly = true)
    public List<ForkliftOperatorResponse> getOperators(
            UUID organizationId, UUID branchId, String status, String licenseStatus, String search) {

        log.debug("Listing forklift operators for organization: {}", organizationId);

        List<ForkliftOperatorEntity> list = (organizationId != null)
                ? operatorRepositoryPort.findByOrganizationId(organizationId)
                : operatorRepositoryPort.findAll();

        return list.stream()
                .filter(op -> branchId == null
                        || (op.getBranch() != null && branchId.equals(op.getBranch().getId())))
                .filter(op -> status == null || status.isBlank()
                        || op.getStatus().name().equalsIgnoreCase(status))
                .filter(op -> licenseStatus == null || licenseStatus.isBlank()
                        || op.getLicenseStatus().name().equalsIgnoreCase(licenseStatus))
                .filter(op -> {
                    if (search == null || search.isBlank()) return true;
                    String q = search.toLowerCase();
                    return op.getFullName().toLowerCase().contains(q)
                            || op.getCode().toLowerCase().contains(q)
                            || op.getLicenseNumberDc3().toLowerCase().contains(q);
                })
                .map(mapper::toResponse)
                .collect(Collectors.toList());
    }

    // ─── DELETE (SOFT) ───────────────────────────────────────────────────────────

    @Override
    @Transactional
    public void deleteOperator(UUID id) {
        log.info("Soft-deleting forklift operator: {}", id);

        ForkliftOperatorEntity existing = operatorRepositoryPort.findActiveById(id)
                .orElseThrow(() -> new EntityNotFoundException(
                        "Montacarguista no encontrado con ID: " + id));

        String currentUser = securityAuditHelper.getCurrentUsername();

        logAudit(currentUser, "FORKLIFT_OPERATOR_DELETED", id, existing, null);

        operatorRepositoryPort.softDeleteById(id, currentUser);
    }

    // ─── STATUS CHANGE ───────────────────────────────────────────────────────────

    @Override
    @Transactional
    public ForkliftOperatorResponse updateOperatorStatus(UUID id, UpdateForkliftOperatorStatusRequest request) {
        ForkliftOperatorStatus newStatus;
        try {
            newStatus = ForkliftOperatorStatus.valueOf(request.getStatus().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new ValidationException("Estatus inválido: " + request.getStatus()
                    + ". Los valores aceptados son: ACTIVO, INACTIVO.");
        }

        log.info("Updating forklift operator {} status to {}", id, newStatus);

        ForkliftOperatorEntity existing = operatorRepositoryPort.findActiveById(id)
                .orElseThrow(() -> new EntityNotFoundException(
                        "Montacarguista no encontrado con ID: " + id));

        ForkliftOperatorEntity snapshot = cloneEntity(existing);
        existing.setStatus(newStatus);

        String currentUser = securityAuditHelper.getCurrentUsername();
        existing.setUpdatedBy(currentUser);

        ForkliftOperatorEntity saved = operatorRepositoryPort.save(existing);

        Map<String, Object> beforeState = buildAuditState(snapshot);
        Map<String, Object> afterState  = buildAuditState(saved);
        if (request.getReason() != null && !request.getReason().isBlank()) {
            afterState.put("reason", request.getReason());
        }
        if (request.getObservations() != null && !request.getObservations().isBlank()) {
            afterState.put("observations", request.getObservations());
        }
        logAuditWithState(currentUser, "FORKLIFT_OPERATOR_STATUS_CHANGED", saved.getId(), beforeState, afterState);

        return mapper.toResponse(saved);
    }

    // ─── AUDIT HISTORY ───────────────────────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public List<ForkliftOperatorAuditResponse> getOperatorAuditLogs(UUID id) {
        log.debug("Fetching audit logs for forklift operator: {}", id);

        if (!operatorRepositoryPort.findActiveById(id).isPresent()) {
            throw new EntityNotFoundException("Montacarguista no encontrado con ID: " + id);
        }

        List<AuditLogEntity> logs = auditLogRepositoryPort.findByEntityTypeAndEntityId(ENTITY_TYPE, id);

        return logs.stream()
                .map(logEntry -> {
                    String username = "SYSTEM";
                    if (logEntry.getUserId() != null) {
                        username = userRepositoryPort.findById(logEntry.getUserId())
                                .map(UserEntity::getUsername)
                                .orElse("UNKNOWN");
                    }

                    List<ForkliftOperatorAuditResponse.AuditDetailResponse> details = logEntry.getDetails().stream()
                            .map(d -> ForkliftOperatorAuditResponse.AuditDetailResponse.builder()
                                    .fieldName(d.getFieldName())
                                    .oldValue(d.getOldValue())
                                    .newValue(d.getNewValue())
                                    .build())
                            .collect(Collectors.toList());

                    return ForkliftOperatorAuditResponse.builder()
                            .logId(logEntry.getLogId())
                            .action(logEntry.getAction())
                            .username(username)
                            .createdAt(logEntry.getCreatedAt())
                            .details(details)
                            .build();
                })
                .collect(Collectors.toList());
    }

    // ─── PRIVATE HELPERS ─────────────────────────────────────────────────────────

    /** Computes the license status from the expiration date. */
    private LicenseStatus computeLicenseStatus(LocalDate expirationDate) {
        if (expirationDate == null) return LicenseStatus.VENCIDA;
        long daysRemaining = LocalDate.now().until(expirationDate).getDays();
        if (daysRemaining < 0)  return LicenseStatus.VENCIDA;
        if (daysRemaining <= 30) return LicenseStatus.POR_VENCER;
        return LicenseStatus.VIGENTE;
    }

    /** Builds the denormalized full name. */
    private String buildFullName(String firstName, String lastNamePaternal, String lastNameMaternal) {
        return (firstName.trim() + " " + lastNamePaternal.trim() + " " + lastNameMaternal.trim()).trim();
    }

    /** Resolves and sets the shift from its UUID, also updating the denormalized shiftName. */
    private void resolveShift(UUID shiftId, ForkliftOperatorEntity entity) {
        if (shiftId == null) {
            entity.setShift(null);
            entity.setShiftName(null);
            return;
        }
        ShiftEntity shift = shiftJpaRepository.findByIdAndIsDeletedFalse(shiftId)
                .orElseThrow(() -> new EntityNotFoundException("Turno no encontrado con ID: " + shiftId));
        entity.setShift(shift);
        entity.setShiftName(shift.getName());
    }

    /** Resolves and sets the branch from its UUID. */
    private void resolveBranch(UUID branchId, ForkliftOperatorEntity entity) {
        if (branchId == null) {
            entity.setBranch(null);
            return;
        }
        BranchEntity branch = branchJpaRepository.findById(branchId)
                .orElseThrow(() -> new EntityNotFoundException("Sucursal no encontrada con ID: " + branchId));
        entity.setBranch(branch);
    }

    /** Creates a shallow clone for audit delta capture. */
    private ForkliftOperatorEntity cloneEntity(ForkliftOperatorEntity source) {
        return ForkliftOperatorEntity.builder()
                .id(source.getId())
                .organization(source.getOrganization())
                .branch(source.getBranch())
                .code(source.getCode())
                .firstName(source.getFirstName())
                .lastNamePaternal(source.getLastNamePaternal())
                .lastNameMaternal(source.getLastNameMaternal())
                .fullName(source.getFullName())
                .licenseNumberDc3(source.getLicenseNumberDc3())
                .licenseExpirationDate(source.getLicenseExpirationDate())
                .licenseStatus(source.getLicenseStatus())
                .shift(source.getShift())
                .shiftName(source.getShiftName())
                .status(source.getStatus())
                .version(source.getVersion())
                .createdAt(source.getCreatedAt())
                .createdBy(source.getCreatedBy())
                .build();
    }

    /** Logs an audit entry using entity states (before/after). */
    private void logAudit(String username, String action, UUID entityId,
                           ForkliftOperatorEntity before, ForkliftOperatorEntity after) {
        try {
            UserEntity actor = userRepositoryPort.findByUsername(username).orElse(null);
            if (actor != null) {
                auditService.log(actor, action, ENTITY_TYPE, entityId,
                        buildAuditState(before), buildAuditState(after));
            }
        } catch (Exception e) {
            log.error("Failed to persist audit log for forklift operator operation: {}", action, e);
        }
    }

    /** Logs an audit entry with pre-built state maps (used when extra fields are appended). */
    private void logAuditWithState(String username, String action, UUID entityId,
                                    Map<String, Object> before, Map<String, Object> after) {
        try {
            UserEntity actor = userRepositoryPort.findByUsername(username).orElse(null);
            if (actor != null) {
                auditService.log(actor, action, ENTITY_TYPE, entityId, before, after);
            }
        } catch (Exception e) {
            log.error("Failed to persist audit log for forklift operator status change", e);
        }
    }

    /** Builds a field-value map representing the state of an entity for audit delta comparison. */
    private Map<String, Object> buildAuditState(ForkliftOperatorEntity entity) {
        if (entity == null) return null;
        Map<String, Object> state = new HashMap<>();
        state.put("id",                    entity.getId() != null ? entity.getId().toString() : null);
        state.put("code",                  entity.getCode());
        state.put("fullName",              entity.getFullName());
        state.put("firstName",             entity.getFirstName());
        state.put("lastNamePaternal",      entity.getLastNamePaternal());
        state.put("lastNameMaternal",      entity.getLastNameMaternal());
        state.put("licenseNumberDc3",      entity.getLicenseNumberDc3());
        state.put("licenseExpirationDate", entity.getLicenseExpirationDate() != null ? entity.getLicenseExpirationDate().toString() : null);
        state.put("licenseStatus",         entity.getLicenseStatus() != null ? entity.getLicenseStatus().name() : null);
        state.put("status",                entity.getStatus() != null ? entity.getStatus().name() : null);
        state.put("shiftId",               entity.getShift() != null ? entity.getShift().getId().toString() : null);
        state.put("shiftName",             entity.getShiftName());
        state.put("organizationId",        entity.getOrganization() != null ? entity.getOrganization().getId().toString() : null);
        state.put("branchId",              entity.getBranch() != null ? entity.getBranch().getId().toString() : null);
        return state;
    }
}
