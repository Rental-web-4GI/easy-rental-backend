package com.yowyob.easyrental.modules.notification.domain.port.out;

import com.yowyob.easyrental.modules.notification.domain.NotificationEntity;
import java.util.List;
import java.util.UUID;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Outgoing port for notification persistence.
 */
public interface NotificationRepositoryPort {

    Mono<NotificationEntity> findById(UUID id);

    Mono<NotificationEntity> save(NotificationEntity notification);

    Mono<Void> deleteById(UUID id);

    Flux<NotificationEntity> findNotificationsByClientId(UUID clientId);

    Mono<Long> countUnreadByClientId(UUID clientId);

    Flux<NotificationEntity> findNotificationsByAgencyId(UUID agencyId);

    Mono<Long> countUnreadByAgencyId(UUID agencyId);

    Flux<NotificationEntity> findNotificationsByAgencyIds(List<UUID> agencyIds);

    Mono<Long> countUnreadByAgencyIds(List<UUID> agencyIds);

    Flux<NotificationEntity> findNotificationsByDriverId(UUID driverId);
}
