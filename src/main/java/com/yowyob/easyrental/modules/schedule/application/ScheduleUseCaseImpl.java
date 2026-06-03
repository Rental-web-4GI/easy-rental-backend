package com.yowyob.easyrental.modules.schedule.application;

import com.yowyob.easyrental.modules.schedule.domain.ScheduleEntity;
import com.yowyob.easyrental.modules.schedule.domain.port.out.ScheduleRepositoryPort;
import com.yowyob.easyrental.shared.dto.ScheduleRequestDTO;
import com.yowyob.easyrental.shared.enums.ResourceType;
import com.yowyob.easyrental.modules.schedule.domain.port.in.ScheduleUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ScheduleUseCaseImpl implements ScheduleUseCase {
    private final ScheduleRepositoryPort scheduleRepository;

    @Transactional
    public Mono<ScheduleEntity> addUnavailability(UUID orgId, ResourceType type, UUID resourceId,
            ScheduleRequestDTO request) {
        ScheduleEntity schedule = ScheduleEntity.builder()
                .id(UUID.randomUUID())
                .organizationId(orgId)
                .resourceType(type)
                .resourceId(resourceId)
                .startDate(request.startDate())
                .endDate(request.endDate())
                .status(request.status().toUpperCase())
                .reason(request.reason())
                .createdAt(LocalDateTime.now())
                .isNewRecord(true)
                .build();

        return scheduleRepository.save(schedule);
    }

    public Flux<ScheduleEntity> getResourceSchedule(ResourceType type, UUID resourceId) {
        return scheduleRepository.findFutureSchedules(type, resourceId, LocalDateTime.now());
    }

    /**
     * Supprime les entrées de planning correspondant à une période et une ressource.
     * Utilisé lors de l'annulation d'une réservation.
     */
    @Transactional
    public Mono<Void> removeScheduleForRental(UUID vehicleId, UUID driverId, LocalDateTime start, LocalDateTime end) {
        // Logique simplifiée : on supprime les schedules qui correspondent exactement aux dates
        // En prod : lier rentalId au schedule pour une suppression précise
        return scheduleRepository.deleteByResourceIdAndDates(vehicleId, start, end)
                .then(driverId != null
                        ? scheduleRepository.deleteByResourceIdAndDates(driverId, start, end)
                        : Mono.empty());
    }
}
