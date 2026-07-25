package com.yowyob.easyrental.modules.inspection.mapper;

import com.yowyob.easyrental.modules.inspection.domain.InspectionItemEntity;
import com.yowyob.easyrental.modules.inspection.domain.RentalInspectionEntity;
import com.yowyob.easyrental.modules.inspection.dto.InspectionItemDTO;
import com.yowyob.easyrental.modules.inspection.dto.InspectionResponseDTO;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Maps inspection domain entities to API response DTOs.
 *
 * @author Easy Rental Team
 * @since 2026-07-25
 */
@Component
public class InspectionMapper {

    public InspectionResponseDTO toResponseDto(RentalInspectionEntity entity, List<InspectionItemEntity> items) {
        return new InspectionResponseDTO(
                entity.getId(),
                entity.getRentalId(),
                entity.getType() != null ? entity.getType().name() : null,
                entity.getOdometer(),
                entity.getFuelLevel(),
                entity.getNotes(),
                entity.getPhotoUrls(),
                entity.getPerformedBy(),
                entity.getPerformedAt(),
                items == null ? List.of() : items.stream().map(this::toItemDto).toList()
        );
    }

    public InspectionItemDTO toItemDto(InspectionItemEntity entity) {
        return new InspectionItemDTO(
                entity.getItemCode(),
                entity.getStatus() != null ? entity.getStatus().name() : null,
                entity.getNote()
        );
    }
}
