package com.fourguard.wms.application.dto.response.history;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class RemissionHistoryPageResponse {
    private final List<RemissionHistoryItemResponse> items;
    private final RemissionHistorySummaryResponse summary;
    private final int page;
    private final int size;
    private final int totalPages;
    private final long totalElements;
}
