package com.yowyob.easyrental.modules.schedule.domain.port.out;

import com.yowyob.easyrental.modules.schedule.domain.ScheduleEntity;
import com.yowyob.easyrental.shared.enums.ResourceType;
import java.time.LocalDateTime;
import java.util.UUID;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Outgoing port for schedule persistence.
 */
public interface ScheduleRepositoryPort {

    Mono<ScheduleEntity> save(ScheduleEntity schedule);

    Flux<ScheduleEntity> findFutureSchedules(ResourceType type, UUID id, LocalDateTime now);

    Flux<ScheduleEntity> findConflictingSchedules(ResourceType type, UUID id, LocalDateTime start, LocalDateTime end);

    Mono<Void> deleteByResourceIdAndDates(UUID resourceId, LocalDateTime start, LocalDateTime end);
}
