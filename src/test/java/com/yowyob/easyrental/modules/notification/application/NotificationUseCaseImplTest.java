package com.yowyob.easyrental.modules.notification.application;

import com.yowyob.easyrental.modules.agency.domain.port.out.AgencyRepositoryPort;
import com.yowyob.easyrental.modules.notification.domain.port.out.NotificationRepositoryPort;
import com.yowyob.easyrental.modules.notification.domain.NotificationEntity;
import com.yowyob.easyrental.modules.notification.domain.NotificationTemplate;
import com.yowyob.easyrental.modules.notification.dto.NotificationResponseDTO;
import com.yowyob.easyrental.modules.notification.mapper.NotificationMapper;
import com.yowyob.easyrental.shared.enums.NotificationReason;
import com.yowyob.easyrental.shared.enums.NotificationResourceType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NotificationUseCaseImplTest {

    @Mock
    private NotificationRepositoryPort notificationRepository;
    @Mock
    private NotificationMapper notificationMapper;
    @Mock
    private AgencyRepositoryPort agencyRepository;

    @InjectMocks
    private NotificationUseCaseImpl notificationUseCase;

    private NotificationResponseDTO sampleDto(UUID id) {
        return new NotificationResponseDTO(id, id, id, "CLIENT", "TEST", null, null,
                LocalDateTime.now(), false, "details");
    }

    @Test
    void shouldCreateNotificationRaw() {
        UUID locationId = UUID.randomUUID();
        UUID resourceId = UUID.randomUUID();
        NotificationEntity entity = NotificationEntity.builder().id(UUID.randomUUID()).details("msg").build();
        NotificationResponseDTO dto = sampleDto(entity.getId());

        when(notificationRepository.save(any())).thenReturn(Mono.just(entity));
        when(notificationMapper.toDto(entity)).thenReturn(dto);

        StepVerifier.create(notificationUseCase.createNotificationRaw(
                        locationId, resourceId, NotificationResourceType.CLIENT,
                        NotificationReason.PAYMENT_RECEIVED, null, null, "Payment received"))
                .expectNextCount(1)
                .verifyComplete();
    }

    @Test
    void shouldGetClientNotifications() {
        UUID clientId = UUID.randomUUID();
        NotificationEntity entity = NotificationEntity.builder().id(UUID.randomUUID()).build();
        when(notificationRepository.findNotificationsByClientId(clientId)).thenReturn(Flux.just(entity));
        when(notificationMapper.toDto(entity)).thenReturn(sampleDto(entity.getId()));

        StepVerifier.create(notificationUseCase.getClientNotifications(clientId))
                .expectNextCount(1)
                .verifyComplete();
    }

    @Test
    void shouldCountUnreadClient() {
        UUID clientId = UUID.randomUUID();
        when(notificationRepository.countUnreadByClientId(clientId)).thenReturn(Mono.just(3L));

        StepVerifier.create(notificationUseCase.countUnreadClient(clientId))
                .expectNext(3L)
                .verifyComplete();
    }

    @Test
    void shouldMarkAsRead() {
        UUID id = UUID.randomUUID();
        NotificationEntity entity = NotificationEntity.builder().id(id).isRead(false).build();
        when(notificationRepository.findById(id)).thenReturn(Mono.just(entity));
        when(notificationRepository.save(any())).thenReturn(Mono.just(entity));

        StepVerifier.create(notificationUseCase.markAsRead(id))
                .verifyComplete();
    }

    @Test
    void shouldDeleteNotification() {
        UUID id = UUID.randomUUID();
        when(notificationRepository.deleteById(id)).thenReturn(Mono.empty());

        StepVerifier.create(notificationUseCase.deleteNotification(id))
                .verifyComplete();
    }

    @Test
    void shouldReturnEmptyOrganizationNotificationsWhenNoAgencies() {
        UUID orgId = UUID.randomUUID();
        when(agencyRepository.findAllByOrganizationId(orgId)).thenReturn(Flux.empty());

        StepVerifier.create(notificationUseCase.getOrganizationNotifications(orgId))
                .verifyComplete();
    }

    @Test
    void shouldCountZeroUnreadForOrganizationWithoutAgencies() {
        UUID orgId = UUID.randomUUID();
        when(agencyRepository.findAllByOrganizationId(orgId)).thenReturn(Flux.empty());

        StepVerifier.create(notificationUseCase.countUnreadOrganization(orgId))
                .expectNext(0L)
                .verifyComplete();
    }

    @Test
    void shouldCreateNotificationFromTemplate() {
        UUID locationId = UUID.randomUUID();
        UUID resourceId = UUID.randomUUID();
        NotificationEntity entity = NotificationEntity.builder().id(UUID.randomUUID()).details("Payment received").build();
        when(notificationRepository.save(any())).thenReturn(Mono.just(entity));
        when(notificationMapper.toDto(entity)).thenReturn(sampleDto(entity.getId()));

        StepVerifier.create(notificationUseCase.createNotification(
                        locationId, resourceId, NotificationResourceType.CLIENT,
                        NotificationReason.PAYMENT_RECEIVED, null, null,
                        NotificationTemplate.PAYMENT_RECEIVED_CLIENT,
                        "1000", "600", "1000", "PARTIAL"))
                .expectNextCount(1)
                .verifyComplete();
    }

    @Test
    void shouldGetAgencyNotifications() {
        UUID agencyId = UUID.randomUUID();
        NotificationEntity entity = NotificationEntity.builder().id(UUID.randomUUID()).build();
        when(notificationRepository.findNotificationsByAgencyId(agencyId)).thenReturn(Flux.just(entity));
        when(notificationMapper.toDto(entity)).thenReturn(sampleDto(entity.getId()));

        StepVerifier.create(notificationUseCase.getAgencyNotifications(agencyId))
                .expectNextCount(1)
                .verifyComplete();
    }

    @Test
    void shouldCountUnreadAgency() {
        UUID agencyId = UUID.randomUUID();
        when(notificationRepository.countUnreadByAgencyId(agencyId)).thenReturn(Mono.just(2L));

        StepVerifier.create(notificationUseCase.countUnreadAgency(agencyId))
                .expectNext(2L)
                .verifyComplete();
    }
}
