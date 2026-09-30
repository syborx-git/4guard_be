package com.fourguard.wms.application.usecase;

import com.fourguard.wms.application.dto.request.map.UpdatePositionStatusMapRequest;
import com.fourguard.wms.application.dto.response.map.*;
import com.fourguard.wms.domain.enums.LocationStatus;
import com.fourguard.wms.domain.ports.in.WarehouseMapUseCase;
import com.fourguard.wms.infrastructure.persistence.entity.LocationEntity;
import com.fourguard.wms.infrastructure.persistence.entity.WarehouseSectionEntity;
import com.fourguard.wms.infrastructure.persistence.repository.CatBlockReasonJpaRepository;
import com.fourguard.wms.infrastructure.persistence.repository.WarehouseMapLocationJpaRepository;
import com.fourguard.wms.infrastructure.persistence.repository.WarehouseMapSectionJpaRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.*;
import java.util.stream.Stream;

@Service
@RequiredArgsConstructor
public class WarehouseMapService implements WarehouseMapUseCase {

    private final WarehouseMapSectionJpaRepository sectionRepository;
    private final WarehouseMapLocationJpaRepository locationRepository;
    private final CatBlockReasonJpaRepository blockReasonRepository;

    private Map<String, List<String>> loadMaterialsMap() {
        Map<String, List<String>> materialsBySection = new HashMap<>();
        try {
            List<Object[]> rows = locationRepository.findAllSectionMaterials();
            for (Object[] row : rows) {
                if (row != null && row.length >= 2 && row[0] != null && row[1] != null) {
                    String secId = row[0].toString();
                    String mat = row[1].toString();
                    materialsBySection.computeIfAbsent(secId, k -> new ArrayList<>()).add(mat);
                }
            }
        } catch (Exception e) {
            // Fallback gracefully si la tabla no tuviera datos
        }
        return materialsBySection;
    }

    @Override
    @Transactional(readOnly = true)
    public WarehouseTopologyResponse getTopology(UUID branchId) {
        List<WarehouseSectionEntity> sections = sectionRepository.findSectionsWithLocationsByBranchId(branchId);
        Map<String, List<String>> materialsMap = loadMaterialsMap();

        int totalPositions = 0;
        int totalCapacity = 0;
        int totalOccupied = 0;
        int totalBlocked = 0;

        List<WarehouseSectionMapResponse> sectionResponses = new ArrayList<>();

        for (WarehouseSectionEntity sec : sections) {
            List<LocationEntity> locs = sec.getLocations() != null ? sec.getLocations() : List.of();
            int posCount = locs.size();
            long blocked = locs.stream().filter(l -> l.getStatus() == LocationStatus.BLOCKED || Boolean.TRUE.equals(l.getIsBlocked())).count();
            long occupied = locs.stream().filter(l -> l.getCurrentOccupancy() != null && l.getCurrentOccupancy() > 0).count();
            int available = (int) (posCount - blocked - occupied);

            totalPositions += posCount;
            totalCapacity += (sec.getCapacidadTarimas() != null ? sec.getCapacidadTarimas() : 0);
            totalOccupied += (int) occupied;
            totalBlocked += (int) blocked;

            int pct = posCount > 0 ? (int) Math.round(((double) occupied / posCount) * 100) : 0;

            String secIdStr = sec.getId() != null ? sec.getId().toString() : "";
            List<String> secMaterials = materialsMap.getOrDefault(secIdStr, Collections.emptyList());

            sectionResponses.add(WarehouseSectionMapResponse.builder()
                .id(sec.getId())
                .code(sec.getCode())
                .name(sec.getName())
                .category(sec.getCategory() != null ? sec.getCategory() : "General")
                .posFijas(sec.getPosFijas() != null ? sec.getPosFijas() : 0)
                .capacidadTarimas(sec.getCapacidadTarimas() != null ? sec.getCapacidadTarimas() : 0)
                .factorEstiba(sec.getFactorEstiba() != null ? sec.getFactorEstiba() : "22 tarimas/pos")
                .materials(secMaterials)
                .notes(sec.getNotes() != null ? sec.getNotes() : "")
                .status(sec.getPosFijas() != null && sec.getPosFijas() > 0 ? "LOADED" : "PENDING")
                .polygonPoints(sec.getPolygonPoints())
                .labelPosition(CoordinateResponse.builder().x(sec.getLabelX()).y(sec.getLabelY()).build())
                .sublabelPosition(CoordinateResponse.builder().x(sec.getSublabelX()).y(sec.getSublabelY()).build())
                .occupiedPositions((int) occupied)
                .availablePositions(Math.max(0, available))
                .blockedPositions((int) blocked)
                .occupancyPercentage(pct)
                .build());
        }

        WarehouseMapStatsResponse stats = WarehouseMapStatsResponse.builder()
            .totalSections(sections.size())
            .loadedSections((int) sections.stream().filter(s -> s.getPosFijas() != null && s.getPosFijas() > 0).count())
            .pendingSections((int) sections.stream().filter(s -> s.getPosFijas() == null || s.getPosFijas() == 0).count())
            .totalPositions(totalPositions)
            .totalCapacityTarimas(totalCapacity)
            .occupiedPositions(totalOccupied)
            .blockedPositions(totalBlocked)
            .build();

        String branchName = sections.isEmpty() || sections.get(0).getBranch() == null ? "CDMX-01" : sections.get(0).getBranch().getName();

        return WarehouseTopologyResponse.builder()
            .branchId(branchId)
            .branchName(branchName)
            .globalStats(stats)
            .sections(sectionResponses)
            .generatedAt(OffsetDateTime.now())
            .build();
    }

    @Override
    @Transactional(readOnly = true)
    public List<PositionMapDetailResponse> getPositionsBySection(UUID sectionId, String status, String search) {
        List<LocationEntity> locations = locationRepository.findBySectionIdOrderByCodeAsc(sectionId);
        Map<String, List<String>> materialsMap = loadMaterialsMap();
        Stream<PositionMapDetailResponse> stream = locations.stream().map(l -> mapLocationToPositionDetail(l, materialsMap));

        if (status != null && !status.isBlank() && !status.equalsIgnoreCase("ALL")) {
            stream = stream.filter(p -> p.getStatus().equalsIgnoreCase(status));
        }

        if (search != null && !search.isBlank()) {
            String query = search.trim().toLowerCase();
            stream = stream.filter(p -> (p.getCode() != null && p.getCode().toLowerCase().contains(query))
                                     || (p.getSectionName() != null && p.getSectionName().toLowerCase().contains(query))
                                     || (p.getSkuCode() != null && p.getSkuCode().toLowerCase().contains(query))
                                     || (p.getSkuDescription() != null && p.getSkuDescription().toLowerCase().contains(query)));
        }

        return stream.toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<PositionMapDetailResponse> getAllPositions(UUID branchId, UUID sectionId, String status, String search) {
        List<LocationEntity> locations = locationRepository.findByBranchIdAndOptionalSectionId(branchId, sectionId);
        Map<String, List<String>> materialsMap = loadMaterialsMap();
        Stream<PositionMapDetailResponse> stream = locations.stream().map(l -> mapLocationToPositionDetail(l, materialsMap));

        if (status != null && !status.isBlank() && !status.equalsIgnoreCase("ALL")) {
            stream = stream.filter(p -> p.getStatus().equalsIgnoreCase(status));
        }

        if (search != null && !search.isBlank()) {
            String query = search.trim().toLowerCase();
            stream = stream.filter(p -> (p.getCode() != null && p.getCode().toLowerCase().contains(query))
                                     || (p.getSectionName() != null && p.getSectionName().toLowerCase().contains(query))
                                     || (p.getSkuCode() != null && p.getSkuCode().toLowerCase().contains(query))
                                     || (p.getSkuDescription() != null && p.getSkuDescription().toLowerCase().contains(query))
                                     || (p.getBatchNumber() != null && p.getBatchNumber().toLowerCase().contains(query)));
        }

        return stream.toList();
    }


    @Override
    @Transactional
    public PositionMapDetailResponse updatePositionStatus(UUID positionId, UpdatePositionStatusMapRequest request, String username) {
        LocationEntity loc = locationRepository.findById(positionId)
            .orElseThrow(() -> new EntityNotFoundException("Ubicación no encontrada con ID: " + positionId));

        String sanitizedUser = (username != null && !username.isBlank()) ? username : "OPERATIONS_DESK";

        switch (request.getTargetAction().toUpperCase()) {
            case "BLOCK" -> {
                loc.setStatus(LocationStatus.BLOCKED);
                loc.setIsBlocked(true);
                String reason = request.getReasonCode() != null ? request.getReasonCode() : "BLOQUEO_ADMINISTRATIVO";
                if (request.getComment() != null && !request.getComment().isBlank()) {
                    reason += " - " + request.getComment().trim();
                }
                loc.setBlockReason(reason);
                loc.setStatusReason(reason);
                loc.setUpdatedBy(sanitizedUser);
            }
            case "RELEASE" -> {
                loc.setStatus(LocationStatus.ACTIVE);
                loc.setIsBlocked(false);
                loc.setBlockReason(null);
                loc.setStatusReason(null);
                loc.setCurrentOccupancy(0);
                loc.setUpdatedBy(sanitizedUser);
            }
            case "OCCUPY" -> {
                loc.setStatus(LocationStatus.ACTIVE);
                loc.setIsBlocked(false);
                loc.setBlockReason(null);
                loc.setCurrentOccupancy(loc.getCapacityUnits() != null ? loc.getCapacityUnits() : 22);
                loc.setUpdatedBy(sanitizedUser);
            }
            default -> throw new IllegalArgumentException("Acción targetAction no soportada: " + request.getTargetAction());
        }

        LocationEntity saved = locationRepository.save(loc);
        Map<String, List<String>> materialsMap = loadMaterialsMap();
        return mapLocationToPositionDetail(saved, materialsMap);
    }

    @Override
    @Transactional(readOnly = true)
    public List<CatBlockReasonResponse> getActiveBlockReasons() {
        return blockReasonRepository.findByIsActiveTrueOrderByDescriptionAsc().stream()
            .map(r -> new CatBlockReasonResponse(r.getCode(), r.getDescription(), r.getCategory()))
            .toList();
    }

    private PositionMapDetailResponse mapLocationToPositionDetail(LocationEntity l, Map<String, List<String>> materialsMap) {
        String visualStatus = "AVAILABLE";
        if (l.getStatus() == LocationStatus.BLOCKED || Boolean.TRUE.equals(l.getIsBlocked())) {
            visualStatus = "BLOCKED";
        } else if (l.getStatus() == LocationStatus.MAINTENANCE) {
            visualStatus = "MAINTENANCE";
        } else if (l.getCurrentOccupancy() != null && l.getCurrentOccupancy() > 0) {
            visualStatus = "OCCUPIED";
        }

        int posNum = 1;
        try {
            if (l.getPosition() != null) posNum = Integer.parseInt(l.getPosition());
        } catch (NumberFormatException ignored) {}

        String skuCode = null;
        String skuDescription = "Sin Material Asignado";

        if (l.getSection() != null && l.getSection().getId() != null) {
            String secIdKey = l.getSection().getId().toString();
            List<String> mats = materialsMap.get(secIdKey);
            if (mats != null && !mats.isEmpty()) {
                int idx = Math.abs(posNum - 1) % mats.size();
                String chosenMat = mats.get(idx);
                int firstSpace = chosenMat.indexOf(' ');
                if (firstSpace > 0) {
                    skuCode = chosenMat.substring(0, firstSpace);
                    skuDescription = chosenMat.substring(firstSpace + 1);
                } else {
                    skuDescription = chosenMat;
                }
            }
        }

        return PositionMapDetailResponse.builder()
            .id(l.getId())
            .positionNumber(posNum)
            .code(l.getCode())
            .sectionId(l.getSection() != null ? l.getSection().getId() : null)
            .sectionName(l.getSection() != null ? l.getSection().getName() : "")
            .skuCode(skuCode)
            .skuDescription(skuDescription)
            .status(visualStatus)
            .capacityTarimas(l.getCapacityUnits() != null ? l.getCapacityUnits() : 22)
            .currentTarimas(l.getCurrentOccupancy() != null ? l.getCurrentOccupancy() : 0)
            .batchNumber("N/A")
            .lastMovement("Sin movimientos")
            .blockReason(l.getBlockReason())
            .statusReason(l.getStatusReason())
            .isBlocked(Boolean.TRUE.equals(l.getIsBlocked()))
            .build();
    }
}
