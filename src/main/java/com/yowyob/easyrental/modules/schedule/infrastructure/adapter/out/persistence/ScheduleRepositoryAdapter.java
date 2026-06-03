package com.yowyob.easyrental.modules.schedule.infrastructure.adapter.out.persistence;

import com.yowyob.easyrental.modules.schedule.domain.ScheduleEntity;
import com.yowyob.easyrental.modules.schedule.domain.port.out.ScheduleRepositoryPort;
import com.yowyob.easyrental.shared.enums.ResourceType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class ScheduleRepositoryAdapter implements ScheduleRepositoryPort {

    private final ScheduleRepository scheduleRepository;

    @Override
    public Mono<ScheduleEntity> save(ScheduleEntity schedule) {
        return scheduleRepository.save(schedule);
    }

    @Override
    public Flux<ScheduleEntity> findFutureSchedules(ResourceType type, UUID id, LocalDateTime now) {
        return scheduleRepository.findFutureSchedules(type, id, now);
    }

    @Override
    public Flux<ScheduleEntity> findConflictingSchedules(
            ResourceType type, UUID id, LocalDateTime start, LocalDateTime end) {
        return scheduleRepository.findConflictingSchedules(type, id, start, end);
    }

    @Override
    public Mono<Void> deleteByResourceIdAndDates(UUID resourceId, LocalDateTime start, LocalDateTime end) {
        return scheduleRepository.deleteByResourceIdAndDates(resourceId, start, end);
    }
}
