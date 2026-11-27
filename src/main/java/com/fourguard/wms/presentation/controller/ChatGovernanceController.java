package com.fourguard.wms.presentation.controller;

import com.fourguard.wms.application.dto.chat.ChatGovernanceDtos.*;
import com.fourguard.wms.application.usecase.ChatGovernanceService;
import com.fourguard.wms.shared.response.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/chat/governance")
@PreAuthorize("hasAnyRole('CEO','DIRECTOR','ADMIN','SUPER_ADMIN')")
@RequiredArgsConstructor
public class ChatGovernanceController {

    private final ChatGovernanceService governanceService;

    @GetMapping("/overview")
    public ResponseEntity<ApiResponse<ChatGovernanceOverviewDto>> getOverview() {
        return ResponseEntity.ok(ApiResponse.ok("Resumen de gobierno obtenido", governanceService.getOverview()));
    }

    @GetMapping("/spaces")
    public ResponseEntity<ApiResponse<PagedResponse<GovernanceSpaceDto>>> listSpaces(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return ResponseEntity.ok(ApiResponse.ok("Espacios obtenidos", governanceService.listSpaces(status, search, page, size)));
    }

    @PatchMapping("/spaces/{id}/status")
    public ResponseEntity<ApiResponse<GovernanceSpaceDto>> updateSpaceStatus(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateSpaceStatusRequest request
    ) {
        return ResponseEntity.ok(ApiResponse.ok("Estado del espacio actualizado", governanceService.updateSpaceStatus(id, request)));
    }

    @GetMapping("/users")
    public ResponseEntity<ApiResponse<PagedResponse<GovernanceChatUserDto>>> listUsers(
            @RequestParam(defaultValue = "ALL") String filter,
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return ResponseEntity.ok(ApiResponse.ok("Usuarios obtenidos", governanceService.listUsers(filter, search, page, size)));
    }

    @PostMapping("/users/{id}/freeze")
    public ResponseEntity<ApiResponse<GovernanceActionResponse>> freezeUser(
            @PathVariable UUID id,
            @Valid @RequestBody FreezeUserRequest request
    ) {
        GovernanceActionResponse res = governanceService.freezeUser(id, request);
        return ResponseEntity.ok(ApiResponse.ok(res.message(), res));
    }

    @PostMapping("/users/{id}/unfreeze")
    public ResponseEntity<ApiResponse<GovernanceActionResponse>> unfreezeUser(
            @PathVariable UUID id
    ) {
        GovernanceActionResponse res = governanceService.unfreezeUser(id);
        return ResponseEntity.ok(ApiResponse.ok(res.message(), res));
    }

    @GetMapping("/audit/messages")
    public ResponseEntity<ApiResponse<PagedResponse<GovernanceAuditMessageDto>>> getAuditMessages(
            @RequestParam(defaultValue = "ALL") String type,
            @RequestParam(defaultValue = "7") int days,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return ResponseEntity.ok(ApiResponse.ok("Auditoría de mensajes obtenida", governanceService.getAuditMessages(type, days, page, size)));
    }

    @GetMapping("/audit/attachments")
    public ResponseEntity<ApiResponse<PagedResponse<GovernanceAttachmentDto>>> getAuditAttachments(
            @RequestParam(defaultValue = "7") int days,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return ResponseEntity.ok(ApiResponse.ok("Auditoría de adjuntos obtenida", governanceService.getAuditAttachments(days, page, size)));
    }

    @GetMapping("/audit/attachments/{attachmentId}/download")
    public ResponseEntity<ApiResponse<GovernanceAttachmentDownloadDto>> downloadAuditAttachment(
            @PathVariable UUID attachmentId
    ) {
        return ResponseEntity.ok(ApiResponse.ok("Adjunto obtenido", governanceService.downloadAuditAttachment(attachmentId)));
    }

    @GetMapping("/dlp/events")
    public ResponseEntity<ApiResponse<PagedResponse<GovernanceDlpEventDto>>> getDlpEvents(
            @RequestParam(defaultValue = "7") int days,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return ResponseEntity.ok(ApiResponse.ok("Eventos DLP obtenidos", governanceService.getDlpEvents(days, page, size)));
    }

    @PostMapping("/break-glass/conversations/{id}")
    public ResponseEntity<ApiResponse<BreakGlassTranscriptDto>> breakGlassInspect(
            @PathVariable UUID id,
            @Valid @RequestBody BreakGlassRequest request
    ) {
        return ResponseEntity.ok(ApiResponse.ok("Inspección Break-Glass registrada", governanceService.breakGlassInspect(id, request)));
    }
}
