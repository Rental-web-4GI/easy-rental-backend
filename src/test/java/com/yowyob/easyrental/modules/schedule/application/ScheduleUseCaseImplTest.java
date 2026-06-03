package com.yowyob.easyrental.modules.schedule.application;

import com.yowyob.easyrental.modules.schedule.domain.port.out.ScheduleRepositoryPort;
import com.yowyob.easyrental.modules.schedule.domain.ScheduleEntity;
import com.yowyob.easyrental.shared.dto.ScheduleRequestDTO;
import com.yowyob.easyrental.shared.enums.ResourceType;
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
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ScheduleUseCaseImplTest {

    @Mock
    private ScheduleRepositoryPort scheduleRepository;

    @InjectMocks
    private ScheduleUseCaseImpl scheduleUseCase;

    @Test
    void shouldAddUnavailability() {
        UUID orgId = UUID.randomUUID();
        UUID resourceId = UUID.randomUUID();
        ScheduleRequestDTO request = new ScheduleRequestDTO(
                LocalDateTime.now().plusDays(1),
                LocalDateTime.now().plusDays(2),
                "UNAVAILABLE",
                "Holiday"
        );
        when(scheduleRepository.save(any())).thenAnswer(inv -> Mono.just(inv.getArgument(0)));

        StepVerifier.create(scheduleUseCase.addUnavailability(orgId, ResourceType.VEHICLE, resourceId, request))
                .expectNextMatches(s -> s.getStatus().equals("UNAVAILABLE"))
                .verifyComplete();
    }

    @Test
    void shouldGetResourceSchedule() {
        UUID resourceId = UUID.randomUUID();
        ScheduleEntity schedule = ScheduleEntity.builder().id(UUID.randomUUID()).resourceId(resourceId).build();
        when(scheduleRepository.findFutureSchedules(eq(ResourceType.VEHICLE), eq(resourceId), any(LocalDateTime.class)))
                .thenReturn(Flux.just(schedule));

        StepVerifier.create(scheduleUseCase.getResourceSchedule(ResourceType.VEHICLE, resourceId))
                .expectNextCount(1)
                .verifyComplete();
    }

    @Test
    void shouldRemoveScheduleForRental() {
        UUID vehicleId = UUID.randomUUID();
        UUID driverId = UUID.randomUUID();
        LocalDateTime start = LocalDateTime.now();
        LocalDateTime end = start.plusDays(1);
        when(scheduleRepository.deleteByResourceIdAndDates(vehicleId, start, end)).thenReturn(Mono.empty());
        when(scheduleRepository.deleteByResourceIdAndDates(driverId, start, end)).thenReturn(Mono.empty());

        StepVerifier.create(scheduleUseCase.removeScheduleForRental(vehicleId, driverId, start, end))
                .verifyComplete();
    }
}
