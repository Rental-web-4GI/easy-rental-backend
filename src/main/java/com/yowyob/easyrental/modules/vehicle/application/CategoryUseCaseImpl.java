package com.yowyob.easyrental.modules.vehicle.application;

import com.yowyob.easyrental.modules.vehicle.domain.VehicleCategoryEntity;
import com.yowyob.easyrental.modules.vehicle.domain.port.in.CategoryUseCase;
import com.yowyob.easyrental.modules.vehicle.dto.CategoryRequestDTO;
import com.yowyob.easyrental.modules.vehicle.dto.CategoryResponseDTO;
import com.yowyob.easyrental.modules.vehicle.domain.port.out.CategoryRepositoryPort;
import com.yowyob.easyrental.shared.exception.ResourceNotFoundException;
import com.yowyob.easyrental.shared.exception.ValidationException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Objects;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CategoryUseCaseImpl implements CategoryUseCase {

    private final CategoryRepositoryPort categoryRepository;

    @Override
    public Mono<CategoryResponseDTO> create(UUID orgId, CategoryRequestDTO request) {
        VehicleCategoryEntity category = VehicleCategoryEntity.builder()
                .id(UUID.randomUUID())
                .organizationId(orgId)
                .name(request.name())
                .description(request.description())
                .isNewRecord(true)
                .build();
        return categoryRepository.save(Objects.requireNonNull(category)).map(this::toDto);
    }

    @Override
    public Flux<CategoryResponseDTO> getByOrg(UUID orgId) {
        return categoryRepository.findAllByOrganizationIdOrSystem(orgId).map(this::toDto);
    }

    @Override
    public Flux<CategoryResponseDTO> getAllCategories() {
        return categoryRepository.findAll().map(this::toDto);
    }

    @Override
    public Flux<CategoryResponseDTO> getByAgency(UUID agencyId) {
        return categoryRepository.findAllByAgencyIdOrSystem(agencyId).map(this::toDto);
    }

    @Override
    public Mono<CategoryResponseDTO> getById(UUID id) {
        return categoryRepository.findById(Objects.requireNonNull(id))
                .map(this::toDto)
                .switchIfEmpty(Mono.error(new ResourceNotFoundException("Category not found")));
    }

    @Override
    public Mono<Void> delete(UUID id) {
        return categoryRepository.findById(Objects.requireNonNull(id))
                .switchIfEmpty(Mono.error(new ResourceNotFoundException("Category not found")))
                .flatMap(cat -> {
                    if (cat.getOrganizationId() == null) {
                        return Mono.error(new ValidationException("Cannot delete system category"));
                    }
                    return categoryRepository.deleteById(Objects.requireNonNull(id));
                });
    }

    @Override
    public Mono<CategoryResponseDTO> update(UUID id, CategoryRequestDTO request) {
        return categoryRepository.findById(Objects.requireNonNull(id))
                .flatMap(existingCat -> {
                    existingCat.setName(request.name());
                    existingCat.setDescription(request.description());
                    return categoryRepository.save(existingCat);
                })
                .map(this::toDto)
                .switchIfEmpty(Mono.error(new ResourceNotFoundException("Category not found")));
    }

    private CategoryResponseDTO toDto(VehicleCategoryEntity entity) {
        return new CategoryResponseDTO(
                entity.getId(),
                entity.getOrganizationId(),
                entity.getName(),
                entity.getDescription()
        );
    }
}
