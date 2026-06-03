package com.yowyob.easyrental.modules.vehicle.infrastructure.adapter.out.persistence;

import com.yowyob.easyrental.modules.vehicle.domain.VehicleCategoryEntity;
import com.yowyob.easyrental.modules.vehicle.domain.port.out.CategoryRepositoryPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.UUID;

@Component
@RequiredArgsConstructor
public class CategoryRepositoryAdapter implements CategoryRepositoryPort {

    private final CategoryRepository categoryRepository;

    @Override
    public Mono<VehicleCategoryEntity> findById(UUID id) {
        return categoryRepository.findById(id);
    }

    @Override
    public Mono<VehicleCategoryEntity> save(VehicleCategoryEntity category) {
        return categoryRepository.save(category);
    }

    @Override
    public Flux<VehicleCategoryEntity> findAll() {
        return categoryRepository.findAll();
    }

    @Override
    public Flux<VehicleCategoryEntity> findAllByOrganizationIdOrSystem(UUID orgId) {
        return categoryRepository.findAllByOrganizationIdOrSystem(orgId);
    }

    @Override
    public Flux<VehicleCategoryEntity> findAllByAgencyIdOrSystem(UUID agencyId) {
        return categoryRepository.findAllByAgencyIdOrSystem(agencyId);
    }

    @Override
    public Mono<UUID> findOrgIdByCategoryId(UUID categoryId) {
        return categoryRepository.findOrgIdByCategoryId(categoryId);
    }

    @Override
    public Mono<Void> deleteById(UUID id) {
        return categoryRepository.deleteById(id);
    }
}
