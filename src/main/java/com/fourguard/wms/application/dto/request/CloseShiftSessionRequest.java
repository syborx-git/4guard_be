package com.fourguard.wms.application.dto.request;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class CloseShiftSessionRequest {
    String reason;
}
