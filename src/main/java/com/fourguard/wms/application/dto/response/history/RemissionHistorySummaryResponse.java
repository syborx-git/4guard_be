package com.fourguard.wms.application.dto.response.history;

import lombok.Builder;
import lombok.Getter;

import java.util.List;
import java.util.UUID;

@Getter
@Builder
public class RemissionHistorySummaryResponse {
    private final long total;
    private final long completed;
    private final long cancelled;
    private final long pending;
    private final List<ClientCount> byClient;
    private final List<ShiftCount> byShift;

    @Getter
    @Builder
    public static class ClientCount {
        private final UUID clientId;
        private final String clientName;
        private final long count;
    }

    @Getter
    @Builder
    public static class ShiftCount {
        private final UUID shiftSessionId;
        private final String shiftName;
        private final long count;
    }
}
