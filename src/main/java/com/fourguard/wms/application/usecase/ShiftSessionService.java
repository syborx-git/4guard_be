package com.fourguard.wms.application.usecase;

import com.fourguard.wms.application.dto.request.CloseShiftSessionRequest;
import com.fourguard.wms.application.dto.request.OpenShiftSessionRequest;
import com.fourguard.wms.application.dto.response.ActiveShiftResponse;
import com.fourguard.wms.application.dto.response.ShiftSessionResponse;
import com.fourguard.wms.domain.enums.ShiftScopeType;
import com.fourguard.wms.domain.enums.ShiftSessionSource;
import com.fourguard.wms.domain.enums.ShiftSessionStatus;
import com.fourguard.wms.domain.exception.EntityNotFoundException;
import com.fourguard.wms.domain.exception.ValidationException;
import com.fourguard.wms.domain.ports.in.ShiftUseCase;
import com.fourguard.wms.domain.ports.out.BranchRepositoryPort;
import com.fourguard.wms.domain.ports.out.ShiftRepositoryPort;
import com.fourguard.wms.domain.ports.out.ShiftSessionRepositoryPort;
import com.fourguard.wms.domain.ports.out.UserRepositoryPort;
import com.fourguard.wms.infrastructure.persistence.entity.BranchEntity;
import com.fourguard.wms.infrastructure.persistence.entity.ShiftEntity;
import com.fourguard.wms.infrastructure.persistence.entity.ShiftSessionEntity;
import com.fourguard.wms.infrastructure.persistence.entity.UserEntity;
import com.fourguard.wms.shared.audit.SecurityAuditHelper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.UUID;

/**
 * Orquesta las sesiones operativas de turno por sucursal.
 * La sesión se abre de forma perezosa (idempotente) al resolver el turno vigente
 * o al crear un movimiento de almacén.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ShiftSessionService {

    private final ShiftSessionRepositoryPort sessionRepositoryPort;
    private final ShiftRepositoryPort shiftRepositoryPort;
    private final BranchRepositoryPort branchRepositoryPort;
    private final UserRepositoryPort userRepositoryPort;
    private final ShiftUseCase shiftUseCase;
    private final SecurityAuditHelper securityAuditHelper;

    /**
     * Devuelve el turno vigente enriquecido con la sesión operativa persistida.
     * Si no existe sesión abierta, la crea (idempotente por sucursal).
     */
    @Transactional
    public ActiveShiftResponse getActiveShiftWithSession(UUID branchId) {
        ActiveShiftResponse base = shiftUseCase.getActiveShift(branchId);
        ShiftSessionEntity session = resolveOrOpen(base);
        return base.toBuilder().sessionId(session.getId()).build();
    }

    /**
     * Sesión abierta de la sucursal, creándola si aún no existe.
     */
    @Transactional
    public ShiftSessionEntity getOrOpenActiveSession(UUID branchId) {
        return sessionRepositoryPort.findOpenByBranchId(branchId)
                .orElseGet(() -> resolveOrOpen(shiftUseCase.getActiveShift(branchId)));
    }

    @Transactional
    public ShiftSessionResponse openSession(OpenShiftSessionRequest request) {
        if (request.getBranchId() == null) {
            throw new ValidationException("La sucursal (branchId) es requerida.");
        }

        ShiftSessionEntity existing = sessionRepositoryPort.findOpenByBranchId(request.getBranchId()).orElse(null);
        if (existing != null) {
            return toResponse(existing);
        }

        ActiveShiftResponse base = shiftUseCase.getActiveShift(request.getBranchId());
        ShiftEntity shift = null;
        ShiftSessionSource source;
        if (request.getShiftId() != null) {
            shift = shiftRepositoryPort.findById(request.getShiftId())
                    .orElseThrow(() -> new EntityNotFoundException("Turno no encontrado con ID: " + request.getShiftId()));
            validateShiftForBranch(shift, request.getBranchId());
            source = ShiftSessionSource.MANUAL;
        } else {
            shift = base.getShiftId() != null
                    ? shiftRepositoryPort.findById(base.getShiftId()).orElse(null)
                    : null;
            source = shift != null ? ShiftSessionSource.AUTO : ShiftSessionSource.FALLBACK;
        }

        return toResponse(createSession(base, shift, source));
    }

    @Transactional
    public ShiftSessionResponse closeSession(UUID sessionId, CloseShiftSessionRequest request) {
        ShiftSessionEntity session = sessionRepositoryPort.findById(sessionId)
                .orElseThrow(() -> new EntityNotFoundException("Sesión de turno no encontrada con ID: " + sessionId));

        if (session.getStatus() == ShiftSessionStatus.OPEN) {
            session.setStatus(ShiftSessionStatus.CLOSED);
            session.setEndedAt(OffsetDateTime.now(ZoneOffset.UTC));
            session.setClosedBy(currentUserId());
            session = sessionRepositoryPort.save(session);
        }

        return toResponse(session);
    }

    private ShiftSessionEntity resolveOrOpen(ActiveShiftResponse base) {
        return sessionRepositoryPort.findOpenByBranchId(base.getBranchId())
                .orElseGet(() -> {
                    ShiftEntity shift = base.getShiftId() != null
                            ? shiftRepositoryPort.findById(base.getShiftId()).orElse(null)
                            : null;
                    ShiftSessionSource source = shift != null
                            ? ShiftSessionSource.AUTO
                            : ShiftSessionSource.FALLBACK;
                    return createSession(base, shift, source);
                });
    }

    private ShiftSessionEntity createSession(ActiveShiftResponse base, ShiftEntity shift, ShiftSessionSource source) {
        BranchEntity branch = branchRepositoryPort.findById(base.getBranchId())
                .orElseThrow(() -> new EntityNotFoundException("Sucursal no encontrada con ID: " + base.getBranchId()));

        ShiftSessionEntity session = ShiftSessionEntity.builder()
                .organization(branch.getOrganization())
                .branch(branch)
                .shift(shift)
                .businessDate(base.getBusinessDate())
                .startedAt(OffsetDateTime.now(ZoneOffset.UTC))
                .status(ShiftSessionStatus.OPEN)
                .source(source)
                .openedBy(currentUserId())
                .build();

        try {
            return sessionRepositoryPort.save(session);
        } catch (DataIntegrityViolationException ex) {
            // Carrera con otra petición: el índice único parcial garantiza una sola sesión OPEN.
            log.warn("Sesión de turno ya abierta por otra petición para sucursal {}", base.getBranchId());
            return sessionRepositoryPort.findOpenByBranchId(base.getBranchId())
                    .orElseThrow(() -> ex);
        }
    }

    private void validateShiftForBranch(ShiftEntity shift, UUID branchId) {
        boolean isGlobal = shift.getScopeType() == ShiftScopeType.GLOBAL && shift.getBranch() == null;
        boolean belongsToBranch = shift.getBranch() != null && shift.getBranch().getId().equals(branchId);
        if (!isGlobal && !belongsToBranch) {
            throw new ValidationException("El turno seleccionado no pertenece a la sucursal indicada.");
        }
    }

    private UUID currentUserId() {
        return userRepositoryPort.findByUsername(securityAuditHelper.getCurrentUsername())
                .map(UserEntity::getId)
                .orElse(null);
    }

    private ShiftSessionResponse toResponse(ShiftSessionEntity session) {
        return ShiftSessionResponse.builder()
                .id(session.getId())
                .organizationId(session.getOrganization() != null ? session.getOrganization().getId() : null)
                .branchId(session.getBranch() != null ? session.getBranch().getId() : null)
                .branchName(session.getBranch() != null ? session.getBranch().getName() : null)
                .shiftId(session.getShift() != null ? session.getShift().getId() : null)
                .shiftName(session.getShift() != null ? session.getShift().getName() : null)
                .businessDate(session.getBusinessDate())
                .startedAt(session.getStartedAt())
                .endedAt(session.getEndedAt())
                .status(session.getStatus())
                .source(session.getSource())
                .openedBy(session.getOpenedBy())
                .closedBy(session.getClosedBy())
                .build();
    }
}
