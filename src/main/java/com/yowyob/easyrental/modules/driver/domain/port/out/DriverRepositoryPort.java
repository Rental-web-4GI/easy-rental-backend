package com.yowyob.easyrental.modules.driver.domain.port.out;

import com.yowyob.easyrental.modules.driver.domain.DriverEntity;
import java.time.LocalDateTime;
import java.util.UUID;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Outgoing port for driver persistence.
 */
public interface DriverRepositoryPort {

    Mono<DriverEntity> findById(UUID id);

    Mono<DriverEntity> save(DriverEntity driver);

    Mono<Void> delete(DriverEntity driver);

    Flux<DriverEntity> findAllByOrganizationId(UUID organizationId);

    Flux<DriverEntity> findAllByAgencyId(UUID agencyId);

    Flux<DriverEntity> findAvailableDrivers(UUID agencyId, LocalDateTime startDate, LocalDateTime endDate);
}
