package com.yowyob.easyrental.modules.vehicle.infrastructure.adapter.out.persistence;

import com.yowyob.easyrental.modules.vehicle.domain.VehicleEntity;
import com.yowyob.easyrental.modules.vehicle.domain.port.out.VehicleRepositoryPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.UUID;

@Component
@RequiredArgsConstructor
public class VehicleRepositoryAdapter implements VehicleRepositoryPort {

    private final VehicleRepository vehicleRepository;

    @Override
    public Mono<VehicleEntity> findById(UUID id) {
        return vehicleRepository.findById(id);
    }

    @Override
    public Mono<VehicleEntity> save(VehicleEntity vehicle) {
        return vehicleRepository.save(vehicle);
    }

    @Override
    public Mono<Void> delete(VehicleEntity vehicle) {
        return vehicleRepository.delete(vehicle);
    }

    @Override
    public Flux<VehicleEntity> findAllByAgencyId(UUID agencyId) {
        return vehicleRepository.findAllByAgencyId(agencyId);
    }

    @Override
    public Flux<VehicleEntity> findAllByOrganizationId(UUID organizationId) {
        return vehicleRepository.findAllByOrganizationId(organizationId);
    }

    @Override
    public Flux<VehicleEntity> findAllByStatut(String statut) {
        return vehicleRepository.findAllByStatut(statut);
    }

    @Override
    public Mono<UUID> findOrgIdByVehicleId(UUID vehicleId) {
        return vehicleRepository.findOrgIdByVehicleId(vehicleId);
    }

    @Override
    public Flux<VehicleEntity> findAllByOrganizationIdAndCategoryId(UUID organizationId, UUID categoryId) {
        return vehicleRepository.findAllByOrganizationIdAndCategoryId(organizationId, categoryId);
    }

    @Override
    public Flux<VehicleEntity> findAllByAgencyIdAndCategoryId(UUID agencyId, UUID categoryId) {
        return vehicleRepository.findAllByAgencyIdAndCategoryId(agencyId, categoryId);
    }

    @Override
    public Flux<VehicleEntity> findAllByAgencyIdAndStatut(UUID agencyId, String statut) {
        return vehicleRepository.findAllByAgencyIdAndStatut(agencyId, statut);
    }

    @Override
    public Flux<VehicleEntity> searchAvailableVehicles(UUID agencyId, UUID categoryId, String keyword) {
        return vehicleRepository.searchAvailableVehicles(agencyId, categoryId, keyword);
    }
}
