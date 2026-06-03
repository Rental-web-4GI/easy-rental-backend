package com.yowyob.easyrental.modules.schedule.domain.port.in;

import com.yowyob.easyrental.modules.schedule.domain.ScheduleEntity;
import com.yowyob.easyrental.shared.dto.ScheduleRequestDTO;
import com.yowyob.easyrental.shared.enums.ResourceType;
import java.time.LocalDateTime;
import java.util.UUID;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Incoming port for schedule use cases.
 *
 * @author Easy Rental Team
 * @since 2026-06-03
 */
public interface ScheduleUseCase {
    Mono<ScheduleEntity> addUnavailability(UUID orgId, ResourceType type, UUID resourceId, ScheduleRequestDTO request);
    Flux<ScheduleEntity> getResourceSchedule(ResourceType type, UUID resourceId);
    Mono<Void> removeScheduleForRental(UUID vehicleId, UUID driverId, LocalDateTime start, LocalDateTime end);
}
