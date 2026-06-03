package com.yowyob.easyrental.modules.vehicle.domain.port.in;

import com.yowyob.easyrental.modules.vehicle.dto.PricingUpdateDTO;
import com.yowyob.easyrental.modules.vehicle.dto.ScheduleUpdateDTO;
import com.yowyob.easyrental.modules.vehicle.dto.VehicleDetailResponseDTO;
import com.yowyob.easyrental.modules.vehicle.dto.VehicleRequestDTO;
import com.yowyob.easyrental.modules.vehicle.dto.VehicleResponseDTO;
import java.util.UUID;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Incoming port for vehicle use cases.
 *
 * @author Easy Rental Team
 * @since 2026-06-03
 */
public interface VehicleUseCase {
    Mono<VehicleResponseDTO> createVehicle(UUID orgId, VehicleRequestDTO request);
    Mono<VehicleDetailResponseDTO> getVehicleDetails(UUID id);
    Mono<VehicleDetailResponseDTO> updateVehiclePricing(UUID id, PricingUpdateDTO request);
    Mono<VehicleDetailResponseDTO> updateVehicleSchedules(UUID id, ScheduleUpdateDTO request);
    Flux<VehicleResponseDTO> getVehiclesByOrg(UUID orgId);
    Flux<VehicleResponseDTO> getVehiclesByAgency(UUID agencyId);
    Flux<VehicleResponseDTO> getAvailableVehicles();
    Flux<VehicleResponseDTO> searchAvailableVehicles(UUID agencyId, UUID categoryId, String keyword);
    Flux<VehicleResponseDTO> getAvailableVehiclesByAgency(UUID agencyId);
    Mono<VehicleResponseDTO> getVehicleById(UUID id);
    Mono<VehicleResponseDTO> updateVehicle(UUID id, VehicleRequestDTO request);
    Mono<VehicleResponseDTO> updateVehicleStatus(UUID id, String status);
    Mono<Void> deleteVehicle(UUID id);
    Flux<VehicleResponseDTO> getVehiclesByOrgAndCategory(UUID orgId, UUID categoryId);
    Flux<VehicleResponseDTO> getVehiclesByAgencyAndCategory(UUID agencyId, UUID categoryId);
}
