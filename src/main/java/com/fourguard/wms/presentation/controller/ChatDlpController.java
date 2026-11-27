package com.fourguard.wms.presentation.controller;

import com.fourguard.wms.application.dto.chat.ChatGovernanceDtos.DlpReportResponse;
import com.fourguard.wms.application.dto.chat.ChatGovernanceDtos.ReportDlpEventRequest;
import com.fourguard.wms.application.usecase.ChatGovernanceService;
import com.fourguard.wms.shared.response.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/chat/dlp")
@PreAuthorize("isAuthenticated()")
@RequiredArgsConstructor
public class ChatDlpController {

    private final ChatGovernanceService governanceService;

    @PostMapping("/report")
    public ResponseEntity<ApiResponse<DlpReportResponse>> reportDlpEvent(
            @Valid @RequestBody ReportDlpEventRequest request,
            HttpServletRequest httpRequest
    ) {
        return ResponseEntity.ok(ApiResponse.ok(
                "Evento de seguridad registrado",
                governanceService.reportDlpEvent(request, httpRequest)
        ));
    }
}
