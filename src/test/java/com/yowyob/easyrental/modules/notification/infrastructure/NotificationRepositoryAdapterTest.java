package com.yowyob.easyrental.modules.notification.infrastructure;

import com.yowyob.easyrental.modules.notification.domain.NotificationEntity;
import com.yowyob.easyrental.modules.notification.infrastructure.adapter.out.persistence.NotificationRepository;
import com.yowyob.easyrental.modules.notification.infrastructure.adapter.out.persistence.NotificationRepositoryAdapter;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.List;
import java.util.UUID;

import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NotificationRepositoryAdapterTest {

    @Mock
    private NotificationRepository notificationRepository;

    @InjectMocks
    private NotificationRepositoryAdapter adapter;

    @Test
    void shouldFindNotificationById() {
        UUID id = UUID.randomUUID();
        NotificationEntity entity = NotificationEntity.builder().id(id).build();
        when(notificationRepository.findById(id)).thenReturn(Mono.just(entity));

        StepVerifier.create(adapter.findById(id))
                .expectNext(entity)
                .verifyComplete();
    }

    @Test
    void shouldSaveNotification() {
        NotificationEntity entity = NotificationEntity.builder().id(UUID.randomUUID()).build();
        when(notificationRepository.save(entity)).thenReturn(Mono.just(entity));

        StepVerifier.create(adapter.save(entity))
                .expectNext(entity)
                .verifyComplete();
    }

    @Test
    void shouldFindNotificationsByClientId() {
        UUID clientId = UUID.randomUUID();
        NotificationEntity entity = NotificationEntity.builder().id(UUID.randomUUID()).build();
        when(notificationRepository.findNotificationsByClientId(clientId)).thenReturn(Flux.just(entity));

        StepVerifier.create(adapter.findNotificationsByClientId(clientId))
                .expectNext(entity)
                .verifyComplete();
    }

    @Test
    void shouldCountUnreadByAgencyIds() {
        List<UUID> agencyIds = List.of(UUID.randomUUID());
        when(notificationRepository.countUnreadByAgencyIds(agencyIds)).thenReturn(Mono.just(3L));

        StepVerifier.create(adapter.countUnreadByAgencyIds(agencyIds))
                .expectNext(3L)
                .verifyComplete();
    }
}
