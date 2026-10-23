package com.fourguard.wms.application.usecase;

import com.fourguard.wms.application.dto.response.history.RemissionHistoryItemResponse;
import com.fourguard.wms.application.dto.response.history.RemissionHistoryPageResponse;
import com.fourguard.wms.application.dto.response.history.RemissionHistorySummaryResponse;
import com.fourguard.wms.domain.enums.OutboundStatus;
import com.fourguard.wms.domain.enums.ReceptionStatus;
import com.fourguard.wms.domain.enums.TransferStatus;
import com.fourguard.wms.domain.ports.in.RemissionHistoryUseCase;
import com.fourguard.wms.domain.ports.out.ShiftSessionRepositoryPort;
import com.fourguard.wms.domain.ports.out.WarehouseOutboundRepositoryPort;
import com.fourguard.wms.domain.ports.out.WarehouseReceptionRepositoryPort;
import com.fourguard.wms.domain.ports.out.WarehouseTransferRepositoryPort;
import com.fourguard.wms.infrastructure.persistence.entity.ShiftSessionEntity;
import com.fourguard.wms.infrastructure.persistence.entity.WarehouseOutboundEntity;
import com.fourguard.wms.infrastructure.persistence.entity.WarehouseReceptionEntity;
import com.fourguard.wms.infrastructure.persistence.entity.WarehouseTransferEntity;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class RemissionHistoryService implements RemissionHistoryUseCase {

    private static final int DEFAULT_PAGE_SIZE = 20;
    private static final int MAX_PAGE_SIZE = 100;
    private static final int DEFAULT_LOOKBACK_DAYS = 30;

    private final WarehouseReceptionRepositoryPort receptionRepositoryPort;
    private final WarehouseOutboundRepositoryPort outboundRepositoryPort;
    private final WarehouseTransferRepositoryPort transferRepositoryPort;
    private final ShiftSessionRepositoryPort shiftSessionRepositoryPort;

    @Override
    @Transactional(readOnly = true)
    public RemissionHistoryPageResponse getRemissions(UUID organizationId,
                                                      UUID branchId,
                                                      OffsetDateTime from,
                                                      OffsetDateTime to,
                                                      UUID clientId,
                                                      String status,
                                                      String entityType,
                                                      UUID shiftSessionId,
                                                      String search,
                                                      int page,
                                                      int size) {
        OffsetDateTime rangeTo = to != null ? to : OffsetDateTime.now(ZoneOffset.UTC);
        OffsetDateTime rangeFrom = from != null ? from : rangeTo.minusDays(DEFAULT_LOOKBACK_DAYS);

        String normalizedType = entityType != null ? entityType.trim().toUpperCase() : null;
        String normalizedStatus = status != null && !status.isBlank() ? status.trim().toUpperCase() : null;
        String cleanSearch = search != null && !search.isBlank() ? search.trim() : null;

        List<RemissionHistoryItemResponse> items = new ArrayList<>();

        boolean includeReceptions = normalizedType == null || "RECEPTION".equals(normalizedType);
        boolean includeOutbounds = normalizedType == null || "OUTBOUND".equals(normalizedType);
        boolean includeTransfers = normalizedType == null || "TRANSFER".equals(normalizedType);

        if (includeReceptions && isStatusValid(normalizedStatus, ReceptionStatus.class)) {
            ReceptionStatus receptionStatus = normalizedStatus != null
                    ? ReceptionStatus.valueOf(normalizedStatus) : null;
            List<WarehouseReceptionEntity> receptions = receptionRepositoryPort.findAll(
                    WarehouseReceptionSpecification.withHistoryFilters(
                            organizationId, branchId, receptionStatus, cleanSearch,
                            shiftSessionId, rangeFrom, rangeTo, clientId));
            items.addAll(receptions.stream().map(this::mapReception).collect(Collectors.toList()));
        }

        if (includeOutbounds && isStatusValid(normalizedStatus, OutboundStatus.class)) {
            OutboundStatus outboundStatus = normalizedStatus != null
                    ? OutboundStatus.valueOf(normalizedStatus) : null;
            List<WarehouseOutboundEntity> outbounds = outboundRepositoryPort.findAll(
                    WarehouseOutboundSpecification.withHistoryFilters(
                            organizationId, branchId, outboundStatus, cleanSearch,
                            shiftSessionId, rangeFrom, rangeTo, clientId));
            items.addAll(outbounds.stream().map(this::mapOutbound).collect(Collectors.toList()));
        }

        // Los traspasos no tienen cliente; si se filtra por cliente, se excluyen del resultado.
        if (includeTransfers && clientId == null && isStatusValid(normalizedStatus, TransferStatus.class)) {
            TransferStatus transferStatus = normalizedStatus != null
                    ? TransferStatus.valueOf(normalizedStatus) : null;
            List<WarehouseTransferEntity> transfers = transferRepositoryPort.findAll(
                    WarehouseTransferSpecification.withHistoryFilters(
                            organizationId, branchId, transferStatus, cleanSearch,
                            shiftSessionId, rangeFrom, rangeTo));
            items.addAll(transfers.stream().map(this::mapTransfer).collect(Collectors.toList()));
        }

        enrichShiftNames(items);
        sortByCreatedAtDesc(items);

        List<RemissionHistoryItemResponse> paginated = paginate(items, page, size);
        RemissionHistorySummaryResponse summary = buildSummary(items);

        int safePage = Math.max(page, 1);
        int safeSize = normalizeSize(size);
        int totalPages = items.isEmpty() ? 1 : (int) Math.ceil((double) items.size() / safeSize);

        return RemissionHistoryPageResponse.builder()
                .items(paginated)
                .summary(summary)
                .page(safePage)
                .size(safeSize)
                .totalPages(totalPages)
                .totalElements(items.size())
                .build();
    }

    private RemissionHistoryItemResponse mapReception(WarehouseReceptionEntity entity) {
        int pallets = entity.getPallets() != null ? entity.getPallets().size() : 0;
        BigDecimal pieces = entity.getPallets() == null ? null : entity.getPallets().stream()
                .map(p -> p.getPieces() != null ? p.getPieces() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return RemissionHistoryItemResponse.builder()
                .id(entity.getId())
                .entityType("RECEPTION")
                .folio(entity.getFolio())
                .remisionNumber(entity.getDocNumber())
                .clientId(entity.getClient() != null ? entity.getClient().getId() : null)
                .clientName(entity.getClient() != null ? entity.getClient().getName() : null)
                .branchId(entity.getBranch() != null ? entity.getBranch().getId() : null)
                .branchName(entity.getBranch() != null ? entity.getBranch().getName() : null)
                .status(entity.getStatus() != null ? entity.getStatus().name() : null)
                .shiftSessionId(entity.getShiftSessionId())
                .operator(entity.getCancelledBy() != null ? entity.getCancelledBy() : entity.getCreatedBy())
                .pallets(pallets)
                .pieces(pieces)
                .createdAt(entity.getCreatedAt())
                .completedAt(entity.getCompletedAt())
                .cancelledAt(entity.getCancelledAt())
                .cancellationReason(entity.getCancellationReason())
                .build();
    }

    private RemissionHistoryItemResponse mapOutbound(WarehouseOutboundEntity entity) {
        return RemissionHistoryItemResponse.builder()
                .id(entity.getId())
                .entityType("OUTBOUND")
                .folio(entity.getFolio())
                .remisionNumber(entity.getRemisionNo())
                .clientId(entity.getClient() != null ? entity.getClient().getId() : null)
                .clientName(entity.getClient() != null ? entity.getClient().getName() : null)
                .branchId(entity.getBranch() != null ? entity.getBranch().getId() : null)
                .branchName(entity.getBranch() != null ? entity.getBranch().getName() : null)
                .status(entity.getStatus() != null ? entity.getStatus().name() : null)
                .shiftSessionId(entity.getShiftSessionId())
                .operator(entity.getCancelledBy() != null ? entity.getCancelledBy() : entity.getCreatedBy())
                .pallets(entity.getTotalPallets())
                .pieces(entity.getTotalPieces())
                .createdAt(entity.getCreatedAt())
                .completedAt(entity.getCompletedAt())
                .cancelledAt(entity.getCancelledAt())
                .cancellationReason(entity.getCancellationReason())
                .build();
    }

    private RemissionHistoryItemResponse mapTransfer(WarehouseTransferEntity entity) {
        return RemissionHistoryItemResponse.builder()
                .id(entity.getId())
                .entityType("TRANSFER")
                .folio(entity.getFolio())
                .remisionNumber(entity.getFolio())
                .clientId(null)
                .clientName(null)
                .branchId(entity.getBranch() != null ? entity.getBranch().getId() : null)
                .branchName(entity.getBranch() != null ? entity.getBranch().getName() : null)
                .status(entity.getStatus() != null ? entity.getStatus().name() : null)
                .shiftSessionId(entity.getShiftSessionId())
                .operator(entity.getCancelledBy() != null ? entity.getCancelledBy() : entity.getCreatedBy())
                .pallets(entity.getTotalPallets())
                .pieces(entity.getTotalPieces())
                .createdAt(entity.getCreatedAt())
                .completedAt(null)
                .cancelledAt(entity.getCancelledAt())
                .cancellationReason(entity.getCancellationReason())
                .build();
    }

    private void enrichShiftNames(List<RemissionHistoryItemResponse> items) {
        List<UUID> sessionIds = items.stream()
                .map(RemissionHistoryItemResponse::getShiftSessionId)
                .filter(id -> id != null)
                .distinct()
                .collect(Collectors.toList());

        if (sessionIds.isEmpty()) {
            return;
        }

        Map<UUID, ShiftSessionEntity> sessions = new HashMap<>();
        shiftSessionRepositoryPort.findByIdIn(sessionIds)
                .forEach(session -> sessions.put(session.getId(), session));

        // Resolución de nombres con un mapa auxiliar (los DTO no son mutables).
        Map<UUID, String> shiftNames = sessions.entrySet().stream()
                .filter(e -> e.getValue().getShift() != null)
                .collect(Collectors.toMap(Map.Entry::getKey, e -> e.getValue().getShift().getName()));

        for (int i = 0; i < items.size(); i++) {
            RemissionHistoryItemResponse item = items.get(i);
            if (item.getShiftSessionId() == null) continue;
            String shiftName = shiftNames.get(item.getShiftSessionId());
            if (shiftName == null) continue;
            items.set(i, RemissionHistoryItemResponse.builder()
                    .id(item.getId())
                    .entityType(item.getEntityType())
                    .folio(item.getFolio())
                    .remisionNumber(item.getRemisionNumber())
                    .clientId(item.getClientId())
                    .clientName(item.getClientName())
                    .branchId(item.getBranchId())
                    .branchName(item.getBranchName())
                    .status(item.getStatus())
                    .shiftSessionId(item.getShiftSessionId())
                    .shiftName(shiftName)
                    .operator(item.getOperator())
                    .pallets(item.getPallets())
                    .pieces(item.getPieces())
                    .createdAt(item.getCreatedAt())
                    .completedAt(item.getCompletedAt())
                    .cancelledAt(item.getCancelledAt())
                    .cancellationReason(item.getCancellationReason())
                    .build());
        }
    }

    private void sortByCreatedAtDesc(List<RemissionHistoryItemResponse> items) {
        items.sort((a, b) -> {
            OffsetDateTime left = a.getCreatedAt();
            OffsetDateTime right = b.getCreatedAt();
            if (left == null && right == null) return 0;
            if (left == null) return 1;
            if (right == null) return -1;
            return right.compareTo(left);
        });
    }

    private List<RemissionHistoryItemResponse> paginate(List<RemissionHistoryItemResponse> items, int page, int size) {
        int safeSize = normalizeSize(size);
        int start = Math.max((Math.max(page, 1) - 1) * safeSize, 0);
        if (start >= items.size()) {
            return List.of();
        }
        int end = Math.min(start + safeSize, items.size());
        return items.subList(start, end);
    }

    private int normalizeSize(int size) {
        if (size <= 0) return DEFAULT_PAGE_SIZE;
        return Math.min(size, MAX_PAGE_SIZE);
    }

    private RemissionHistorySummaryResponse buildSummary(List<RemissionHistoryItemResponse> items) {
        long completed = items.stream().filter(i -> "COMPLETED".equals(i.getStatus())).count();
        long cancelled = items.stream().filter(i -> "CANCELLED".equals(i.getStatus())).count();
        long pending = items.size() - completed - cancelled;

        Map<UUID, Long> clientCounts = new LinkedHashMap<>();
        Map<UUID, String> clientNames = new HashMap<>();
        Map<UUID, Long> shiftCounts = new LinkedHashMap<>();
        Map<UUID, String> shiftNames = new HashMap<>();

        for (RemissionHistoryItemResponse item : items) {
            if (item.getClientId() != null) {
                clientCounts.merge(item.getClientId(), 1L, Long::sum);
                clientNames.putIfAbsent(item.getClientId(), item.getClientName());
            }
            if (item.getShiftSessionId() != null) {
                shiftCounts.merge(item.getShiftSessionId(), 1L, Long::sum);
                shiftNames.putIfAbsent(item.getShiftSessionId(), item.getShiftName());
            }
        }

        List<RemissionHistorySummaryResponse.ClientCount> byClient = clientCounts.entrySet().stream()
                .sorted(Map.Entry.<UUID, Long>comparingByValue(Comparator.reverseOrder()))
                .map(e -> RemissionHistorySummaryResponse.ClientCount.builder()
                        .clientId(e.getKey())
                        .clientName(clientNames.get(e.getKey()))
                        .count(e.getValue())
                        .build())
                .collect(Collectors.toList());

        List<RemissionHistorySummaryResponse.ShiftCount> byShift = shiftCounts.entrySet().stream()
                .sorted(Map.Entry.<UUID, Long>comparingByValue(Comparator.reverseOrder()))
                .map(e -> RemissionHistorySummaryResponse.ShiftCount.builder()
                        .shiftSessionId(e.getKey())
                        .shiftName(shiftNames.get(e.getKey()))
                        .count(e.getValue())
                        .build())
                .collect(Collectors.toList());

        return RemissionHistorySummaryResponse.builder()
                .total(items.size())
                .completed(completed)
                .cancelled(cancelled)
                .pending(pending)
                .byClient(byClient)
                .byShift(byShift)
                .build();
    }

    private boolean isStatusValid(String status, Class<? extends Enum<?>> enumType) {
        if (status == null) return true;
        for (Enum<?> value : enumType.getEnumConstants()) {
            if (value.name().equals(status)) return true;
        }
        return false;
    }
}
