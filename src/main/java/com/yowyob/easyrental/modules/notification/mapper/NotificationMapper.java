package com.yowyob.easyrental.modules.notification.mapper;

import com.yowyob.easyrental.modules.notification.domain.NotificationEntity;
import com.yowyob.easyrental.modules.notification.dto.NotificationResponseDTO;
import org.springframework.stereotype.Component;

@Component
public class NotificationMapper {

    public NotificationResponseDTO toDto(NotificationEntity entity) {
        return new NotificationResponseDTO(
                entity.getId(),
                entity.getLocationId(),
                entity.getResourceId(),
                entity.getResourceType(),
                entity.getReason(),
                entity.getVehicleId(),
                entity.getDriverId(),
                entity.getCreatedAt(),
                entity.getIsRead(),
                entity.getDetails()
        );
    }

    public NotificationEntity toEntity(String resourceType, String reason, 
                                       java.util.UUID locationId, java.util.UUID resourceId, 
                                       java.util.UUID vehicleId, java.util.UUID driverId,
                                       String details) {
        return NotificationEntity.builder()
                .id(java.util.UUID.randomUUID())
                .locationId(locationId)
                .resourceId(resourceId)
                .resourceType(resourceType)
                .reason(reason)
                .vehicleId(vehicleId)
                .driverId(driverId)
                .createdAt(java.time.LocalDateTime.now())
                .isRead(false)
                .details(details)
                .isNewRecord(true)
                .build();
    }
}
