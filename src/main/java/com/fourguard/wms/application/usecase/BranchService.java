package com.fourguard.wms.application.usecase;

import com.fourguard.wms.application.dto.request.CreateBranchRequest;
import com.fourguard.wms.application.dto.request.UpdateBranchRequest;
import com.fourguard.wms.application.dto.response.BranchResponse;
import com.fourguard.wms.application.dto.response.audit.BranchAuditResponse;
import com.fourguard.wms.application.mapper.BranchMapper;
import com.fourguard.wms.domain.exception.EntityNotFoundException;
import com.fourguard.wms.domain.exception.ValidationException;
import com.fourguard.wms.domain.ports.in.CreateBranchUseCase;
import com.fourguard.wms.domain.ports.in.DeleteBranchUseCase;
import com.fourguard.wms.domain.ports.in.GetBranchUseCase;
import com.fourguard.wms.domain.ports.in.UpdateBranchUseCase;
import com.fourguard.wms.domain.ports.out.AuditLogRepositoryPort;
import com.fourguard.wms.domain.ports.out.BranchRepositoryPort;
import com.fourguard.wms.domain.ports.out.OrganizationRepositoryPort;
import com.fourguard.wms.domain.ports.out.UserRepositoryPort;
import com.fourguard.wms.infrastructure.persistence.entity.AuditLogEntity;
import com.fourguard.wms.infrastructure.persistence.entity.BranchEntity;
import com.fourguard.wms.infrastructure.persistence.entity.OrganizationEntity;
import com.fourguard.wms.infrastructure.persistence.entity.UserEntity;
import com.fourguard.wms.shared.audit.AuditService;
import com.fourguard.wms.shared.audit.SecurityAuditHelper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class BranchService implements CreateBranchUseCase, UpdateBranchUseCase, GetBranchUseCase, DeleteBranchUseCase {

    private final BranchRepositoryPort branchRepositoryPort;
    private final OrganizationRepositoryPort organizationRepositoryPort;
    private final BranchMapper branchMapper;
    private final SecurityAuditHelper securityAuditHelper;
    private final AuditService auditService;
    private final AuditLogRepositoryPort auditLogRepositoryPort;
    private final UserRepositoryPort userRepositoryPort;

    @Override
    @Transactional
    public BranchResponse createBranch(CreateBranchRequest request) {
        log.info("Creating branch with code: {} under org: {}", request.getCode(), request.getOrganizationId());
        OrganizationEntity organization = organizationRepositoryPort.findById(request.getOrganizationId())
                .orElseThrow(() -> new EntityNotFoundException("Organización no encontrada con ID: " + request.getOrganizationId()));

        if (branchRepositoryPort.existsByOrganizationIdAndCode(request.getOrganizationId(), request.getCode())) {
            throw new ValidationException("El código de sucursal ya existe para esta organización: " + request.getCode());
        }

        BranchEntity entity = branchMapper.toEntity(request);
        entity.setOrganization(organization);
        String currentUser = securityAuditHelper.getCurrentUsername();
        entity.setCreatedBy(currentUser);
        BranchEntity saved = branchRepositoryPort.save(entity);

        // Audit log
        Map<String, Object> afterState = buildAuditState(saved);
        logAuditChange(currentUser, "BRANCH_CREATED", saved.getId(), null, afterState);

        return branchMapper.toResponse(saved);
    }

    @Override
    @Transactional
    public BranchResponse updateBranch(UpdateBranchRequest request) {
        log.info("Updating branch with ID: {}", request.getId());
        BranchEntity existing = branchRepositoryPort.findById(request.getId())
                .orElseThrow(() -> new EntityNotFoundException("Sucursal no encontrada con ID: " + request.getId()));

        OrganizationEntity organization = organizationRepositoryPort.findById(request.getOrganizationId())
                .orElseThrow(() -> new EntityNotFoundException("Organización no encontrada con ID: " + request.getOrganizationId()));

        if (!existing.getCode().equals(request.getCode()) &&
                branchRepositoryPort.existsByOrganizationIdAndCode(request.getOrganizationId(), request.getCode())) {
            throw new ValidationException("El código de sucursal ya existe para esta organización: " + request.getCode());
        }

        Map<String, Object> beforeState = buildAuditState(existing);

        branchMapper.updateEntityFromDto(request, existing);
        existing.setOrganization(organization);
        String currentUser = securityAuditHelper.getCurrentUsername();
        existing.setUpdatedBy(currentUser);
        BranchEntity saved = branchRepositoryPort.save(existing);
        Map<String, Object> afterState = buildAuditState(saved);

        // Audit log
        logAuditChange(currentUser, "BRANCH_UPDATED", saved.getId(), beforeState, afterState);

        return branchMapper.toResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public BranchResponse getBranchById(UUID id) {
        log.debug("Fetching branch with ID: {}", id);
        return branchRepositoryPort.findById(id)
                .map(branchMapper::toResponse)
                .orElseThrow(() -> new EntityNotFoundException("Sucursal no encontrada con ID: " + id));
    }

    @Override
    @Transactional(readOnly = true)
    public List<BranchResponse> getBranchesByOrganizationId(UUID organizationId) {
        log.debug("Fetching branches by org ID: {}", organizationId);
        return branchRepositoryPort.findByOrganizationId(organizationId).stream()
                .map(branchMapper::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<BranchResponse> getAllBranches() {
        log.debug("Fetching all branches");
        return branchRepositoryPort.findAll().stream()
                .map(branchMapper::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<BranchAuditResponse> getBranchAuditLogs(UUID id) {
        log.debug("Fetching audit logs for branch: {}", id);
        if (!branchRepositoryPort.findById(id).isPresent()) {
            throw new EntityNotFoundException("Sucursal no encontrada con ID: " + id);
        }

        List<AuditLogEntity> logs = auditLogRepositoryPort.findByEntityTypeAndEntityId("BRANCH", id);

        return logs.stream()
                .map(logEntry -> {
                    String username = "SYSTEM";
                    if (logEntry.getUserId() != null) {
                        username = userRepositoryPort.findById(logEntry.getUserId())
                                .map(UserEntity::getUsername)
                                .orElse("UNKNOWN");
                    }
                    List<BranchAuditResponse.AuditDetailResponse> detailResponses = logEntry.getDetails().stream()
                            .map(d -> BranchAuditResponse.AuditDetailResponse.builder()
                                    .fieldName(d.getFieldName())
                                    .oldValue(d.getOldValue())
                                    .newValue(d.getNewValue())
                                    .build())
                            .collect(Collectors.toList());

                    return BranchAuditResponse.builder()
                            .logId(logEntry.getLogId())
                            .action(logEntry.getAction())
                            .username(username)
                            .createdAt(logEntry.getCreatedAt())
                            .details(detailResponses)
                            .build();
                })
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void deleteBranch(UUID id) {
        log.info("Deleting branch with ID: {}", id);
        BranchEntity existing = branchRepositoryPort.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Sucursal no encontrada con ID: " + id));

        Map<String, Object> beforeState = buildAuditState(existing);
        String currentUser = securityAuditHelper.getCurrentUsername();

        branchRepositoryPort.deleteById(id);

        // Audit log
        logAuditChange(currentUser, "BRANCH_DELETED", id, beforeState, null);
    }

    // ── Audit Helpers ─────────────────────────────────────────────────────────

    private Map<String, Object> buildAuditState(BranchEntity entity) {
        if (entity == null) return null;
        Map<String, Object> state = new HashMap<>();
        state.put("name", entity.getName());
        state.put("code", entity.getCode());
        state.put("timezone", entity.getTimezone());
        state.put("addressLine1", entity.getAddressLine1());
        state.put("status", entity.getStatus() != null ? entity.getStatus().name() : null);
        if (entity.getOrganization() != null) {
            state.put("organizationId", entity.getOrganization().getId());
            state.put("organizationName", entity.getOrganization().getName());
        }
        return state;
    }

    private void logAuditChange(String username, String action, UUID entityId, Map<String, Object> beforeState, Map<String, Object> afterState) {
        try {
            UserEntity actor = userRepositoryPort.findByUsername(username).orElse(null);
            if (actor != null) {
                auditService.log(actor, action, "BRANCH", entityId, beforeState, afterState);
            }
        } catch (Exception e) {
            log.error("Failed to persist audit log for branch operation", e);
        }
    }
}
