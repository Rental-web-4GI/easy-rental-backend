package com.yowyob.easyrental.modules.schedule.infrastructure.adapter.out.persistence;

import com.yowyob.easyrental.modules.schedule.domain.ScheduleEntity;
import com.yowyob.easyrental.shared.enums.ResourceType;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.UUID;

public interface ScheduleRepository extends R2dbcRepository<ScheduleEntity, UUID> {

    // Récupérer le planning futur et présent
    @Query("SELECT * FROM schedules WHERE resource_type = :type AND resource_id = :id AND end_date >= :now ORDER BY start_date ASC")
    Flux<ScheduleEntity> findFutureSchedules(ResourceType type, UUID id, LocalDateTime now);

    // Vérifier les conflits de dates
    @Query("SELECT * FROM schedules WHERE resource_type = :type AND resource_id = :id AND status IN ('UNAVAILABLE', 'RENTED', 'MAINTENANCE') AND (:start < end_date AND :end > start_date)")
    Flux<ScheduleEntity> findConflictingSchedules(ResourceType type, UUID id, LocalDateTime start, LocalDateTime end);

    @org.springframework.data.r2dbc.repository.Modifying
    @Query("DELETE FROM schedules WHERE resource_id = :resourceId AND start_date = :start AND end_date = :end")
    Mono<Void> deleteByResourceIdAndDates(UUID resourceId, LocalDateTime start, LocalDateTime end);
}
