package com.yowyob.easyrental.modules.tracking.mapper;

import com.yowyob.easyrental.modules.tracking.domain.RentalPositionEntity;
import com.yowyob.easyrental.modules.tracking.dto.PositionResponseDTO;
import org.springframework.stereotype.Component;

/**
 * Maps {@link RentalPositionEntity} to its outgoing DTO representation.
 *
 * @author Easy Rental Team
 * @since 2026-07-25
 */
@Component
public class TrackingMapper {

    public PositionResponseDTO toDto(RentalPositionEntity e) {
        return new PositionResponseDTO(
                e.getId(), e.getRentalId(), e.getVehicleId(),
                e.getLatitude(), e.getLongitude(),
                e.getRecordedAt(),
                e.getSource() == null ? null : e.getSource().name()
        );
    }
}
