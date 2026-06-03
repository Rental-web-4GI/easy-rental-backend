package com.yowyob.easyrental.modules.driver.infrastructure.adapter.out.persistence;

import com.yowyob.easyrental.modules.driver.domain.DriverEntity;
import com.yowyob.easyrental.modules.driver.domain.port.out.DriverRepositoryPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class DriverRepositoryAdapter implements DriverRepositoryPort {

    private final DriverRepository driverRepository;

    @Override
    public Mono<DriverEntity> findById(UUID id) {
        return driverRepository.findById(id);
    }

    @Override
    public Mono<DriverEntity> save(DriverEntity driver) {
        return driverRepository.save(driver);
    }

    @Override
    public Mono<Void> delete(DriverEntity driver) {
        return driverRepository.delete(driver);
    }

    @Override
    public Flux<DriverEntity> findAllByOrganizationId(UUID organizationId) {
        return driverRepository.findAllByOrganizationId(organizationId);
    }

    @Override
    public Flux<DriverEntity> findAllByAgencyId(UUID agencyId) {
        return driverRepository.findAllByAgencyId(agencyId);
    }

    @Override
    public Flux<DriverEntity> findAvailableDrivers(UUID agencyId, LocalDateTime startDate, LocalDateTime endDate) {
        return driverRepository.findAvailableDrivers(agencyId, startDate, endDate);
    }
}
