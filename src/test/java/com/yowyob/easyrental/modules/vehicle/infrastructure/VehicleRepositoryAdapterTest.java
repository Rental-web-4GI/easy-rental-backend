package com.yowyob.easyrental.modules.vehicle.infrastructure;

import com.yowyob.easyrental.modules.vehicle.domain.VehicleEntity;
import com.yowyob.easyrental.modules.vehicle.infrastructure.adapter.out.persistence.VehicleRepository;
import com.yowyob.easyrental.modules.vehicle.infrastructure.adapter.out.persistence.VehicleRepositoryAdapter;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.UUID;

import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class VehicleRepositoryAdapterTest {

    @Mock
    private VehicleRepository vehicleRepository;

    @InjectMocks
    private VehicleRepositoryAdapter adapter;

    @Test
    void shouldFindVehicleById() {
        UUID id = UUID.randomUUID();
        VehicleEntity entity = VehicleEntity.builder().id(id).build();
        when(vehicleRepository.findById(id)).thenReturn(Mono.just(entity));

        StepVerifier.create(adapter.findById(id))
                .expectNext(entity)
                .verifyComplete();
    }

    @Test
    void shouldSaveVehicle() {
        VehicleEntity entity = VehicleEntity.builder().id(UUID.randomUUID()).build();
        when(vehicleRepository.save(entity)).thenReturn(Mono.just(entity));

        StepVerifier.create(adapter.save(entity))
                .expectNext(entity)
                .verifyComplete();
    }

    @Test
    void shouldFindAllByAgencyId() {
        UUID agencyId = UUID.randomUUID();
        VehicleEntity entity = VehicleEntity.builder().id(UUID.randomUUID()).agencyId(agencyId).build();
        when(vehicleRepository.findAllByAgencyId(agencyId)).thenReturn(Flux.just(entity));

        StepVerifier.create(adapter.findAllByAgencyId(agencyId))
                .expectNext(entity)
                .verifyComplete();
    }

    @Test
    void shouldFindOrgIdByVehicleId() {
        UUID vehicleId = UUID.randomUUID();
        UUID orgId = UUID.randomUUID();
        when(vehicleRepository.findOrgIdByVehicleId(vehicleId)).thenReturn(Mono.just(orgId));

        StepVerifier.create(adapter.findOrgIdByVehicleId(vehicleId))
                .expectNext(orgId)
                .verifyComplete();
    }
}
