package com.yowyob.easyrental.modules.notification.domain.port.in;

import com.yowyob.easyrental.modules.notification.domain.NotificationTemplate;
import com.yowyob.easyrental.modules.notification.dto.NotificationResponseDTO;
import com.yowyob.easyrental.shared.enums.NotificationReason;
import com.yowyob.easyrental.shared.enums.NotificationResourceType;
import java.util.UUID;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Incoming port for notification use cases.
 *
 * @author Easy Rental Team
 * @since 2026-06-03
 */
public interface NotificationUseCase {
    Mono<NotificationResponseDTO> createNotification(UUID locationId,
            UUID resourceId,
            NotificationResourceType resourceType,
            NotificationReason reason,
            UUID vehicleId,
            UUID driverId,
            NotificationTemplate template,
            Object... args);
    Mono<NotificationResponseDTO> createNotificationRaw(UUID locationId,
            UUID resourceId,
            NotificationResourceType resourceType,
            NotificationReason reason,
            UUID vehicleId,
            UUID driverId,
            String details);
    Flux<NotificationResponseDTO> getClientNotifications(UUID clientId);
    Mono<Long> countUnreadClient(UUID clientId);
    Flux<NotificationResponseDTO> getAgencyNotifications(UUID agencyId);
    Mono<Long> countUnreadAgency(UUID agencyId);
    Flux<NotificationResponseDTO> getOrganizationNotifications(UUID orgId);
    Mono<Long> countUnreadOrganization(UUID orgId);
    Mono<Void> markAsRead(UUID notificationId);
    Mono<Void> deleteNotification(UUID notificationId);
}
