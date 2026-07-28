package com.yowyob.easyrental.modules.rating.mapper;

import com.yowyob.easyrental.modules.rating.domain.RatingEntity;
import com.yowyob.easyrental.modules.rating.dto.RatingResponseDTO;
import org.springframework.stereotype.Component;

/**
 * Maps rating domain entities to API response DTOs.
 *
 * @author Easy Rental Team
 * @since 2026-07-25
 */
@Component
public class RatingMapper {

    public RatingResponseDTO toDto(RatingEntity entity) {
        return new RatingResponseDTO(
                entity.getId(),
                entity.getRentalId(),
                entity.getRaterType() != null ? entity.getRaterType().name() : null,
                entity.getRaterId(),
                entity.getTargetType() != null ? entity.getTargetType().name() : null,
                entity.getTargetId(),
                entity.getStars(),
                entity.getComment(),
                entity.getCreatedAt()
        );
    }
}
