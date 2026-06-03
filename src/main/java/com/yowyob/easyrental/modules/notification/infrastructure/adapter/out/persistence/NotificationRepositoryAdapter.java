package com.yowyob.easyrental.modules.notification.infrastructure.adapter.out.persistence;

import com.yowyob.easyrental.modules.notification.domain.NotificationEntity;
import com.yowyob.easyrental.modules.notification.domain.port.out.NotificationRepositoryPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class NotificationRepositoryAdapter implements NotificationRepositoryPort {

    private final NotificationRepository notificationRepository;

    @Override
    public Mono<NotificationEntity> findById(UUID id) {
        return notificationRepository.findById(id);
    }

    @Override
    public Mono<NotificationEntity> save(NotificationEntity notification) {
        return notificationRepository.save(notification);
    }

    @Override
    public Mono<Void> deleteById(UUID id) {
        return notificationRepository.deleteById(id);
    }

    @Override
    public Flux<NotificationEntity> findNotificationsByClientId(UUID clientId) {
        return notificationRepository.findNotificationsByClientId(clientId);
    }

    @Override
    public Mono<Long> countUnreadByClientId(UUID clientId) {
        return notificationRepository.countUnreadByClientId(clientId);
    }

    @Override
    public Flux<NotificationEntity> findNotificationsByAgencyId(UUID agencyId) {
        return notificationRepository.findNotificationsByAgencyId(agencyId);
    }

    @Override
    public Mono<Long> countUnreadByAgencyId(UUID agencyId) {
        return notificationRepository.countUnreadByAgencyId(agencyId);
    }

    @Override
    public Flux<NotificationEntity> findNotificationsByAgencyIds(List<UUID> agencyIds) {
        return notificationRepository.findNotificationsByAgencyIds(agencyIds);
    }

    @Override
    public Mono<Long> countUnreadByAgencyIds(List<UUID> agencyIds) {
        return notificationRepository.countUnreadByAgencyIds(agencyIds);
    }

    @Override
    public Flux<NotificationEntity> findNotificationsByDriverId(UUID driverId) {
        return notificationRepository.findNotificationsByDriverId(driverId);
    }
}
