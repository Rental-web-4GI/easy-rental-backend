package com.yowyob.easyrental.modules.driver.domain.port.in;

import com.yowyob.easyrental.modules.driver.dto.DriverDetailResponseDTO;
import com.yowyob.easyrental.modules.driver.dto.DriverResponseDTO;
import com.yowyob.easyrental.modules.vehicle.dto.PricingUpdateDTO;
import com.yowyob.easyrental.modules.vehicle.dto.ScheduleUpdateDTO;
import java.time.LocalDateTime;
import java.util.UUID;
import org.springframework.http.codec.multipart.FilePart;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Incoming port for driver use cases.
 *
 * @author Easy Rental Team
 * @since 2026-06-03
 */
public interface DriverUseCase {
    Mono<DriverResponseDTO> createDriver(UUID orgId,
            UUID agencyId,
            String firstname, String lastname, String tel, Integer age, Integer gender,
            FilePart profilFile, FilePart cniFile, FilePart licenseFile);
    Flux<DriverResponseDTO> getDriversByOrg(UUID orgId);
    Mono<DriverDetailResponseDTO> getDriverDetails(UUID id);
    Flux<DriverResponseDTO> getDriversByAgency(UUID agencyId);
    Flux<DriverResponseDTO> getAvailableDrivers(UUID agencyId, LocalDateTime startDate, LocalDateTime endDate);
    Mono<DriverResponseDTO> getDriverById(UUID id);
    Mono<DriverResponseDTO> changeAgency(UUID driverId, UUID newAgencyId);
    Mono<DriverDetailResponseDTO> updateDriverPricing(UUID id, PricingUpdateDTO request);
    Mono<DriverDetailResponseDTO> updateDriverSchedules(UUID id, ScheduleUpdateDTO request);
    Mono<Void> deleteDriver(UUID id);
}
