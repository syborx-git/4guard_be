package com.fourguard.wms.domain.ports.in;

import com.fourguard.wms.application.dto.request.AlertConfigFilterRequest;
import com.fourguard.wms.application.dto.request.CreateAlertConfigRequest;
import com.fourguard.wms.application.dto.request.UpdateAlertConfigRequest;
import com.fourguard.wms.application.dto.request.UpdateAlertConfigStatusRequest;
import com.fourguard.wms.application.dto.response.AlertConfigResponse;
import com.fourguard.wms.application.dto.response.audit.AlertConfigAuditResponse;

import java.util.List;
import java.util.UUID;

public interface AlertConfigUseCase {

    AlertConfigResponse createAlertConfig(CreateAlertConfigRequest request);

    AlertConfigResponse updateAlertConfig(UUID id, UpdateAlertConfigRequest request);

    AlertConfigResponse updateAlertConfigStatus(UUID id, UpdateAlertConfigStatusRequest request);

    AlertConfigResponse getAlertConfigById(UUID id);

    List<AlertConfigResponse> getAlertConfigs(AlertConfigFilterRequest filter);

    void deleteAlertConfig(UUID id);

    List<AlertConfigAuditResponse> getAlertConfigAuditLogs(UUID id);
}
