package com.yowyob.easyrental.modules.vehicle.application;

import com.yowyob.easyrental.modules.vehicle.domain.port.out.CategoryRepositoryPort;
import com.yowyob.easyrental.modules.vehicle.domain.VehicleCategoryEntity;
import com.yowyob.easyrental.modules.vehicle.dto.CategoryRequestDTO;
import com.yowyob.easyrental.shared.exception.ResourceNotFoundException;
import com.yowyob.easyrental.shared.exception.ValidationException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CategoryUseCaseImplTest {

    @Mock
    private CategoryRepositoryPort categoryRepository;

    @InjectMocks
    private CategoryUseCaseImpl categoryUseCase;

    @Test
    void shouldCreateCategory() {
        UUID orgId = UUID.randomUUID();
        CategoryRequestDTO request = new CategoryRequestDTO("SUV", "Sport utility");
        VehicleCategoryEntity saved = VehicleCategoryEntity.builder()
                .id(UUID.randomUUID()).organizationId(orgId).name("SUV").description("Sport utility").build();
        when(categoryRepository.save(any())).thenReturn(Mono.just(saved));

        StepVerifier.create(categoryUseCase.create(orgId, request))
                .expectNextMatches(dto -> dto.name().equals("SUV"))
                .verifyComplete();
    }

    @Test
    void shouldReturnErrorWhenCategoryNotFound() {
        UUID id = UUID.randomUUID();
        when(categoryRepository.findById(id)).thenReturn(Mono.empty());

        StepVerifier.create(categoryUseCase.getById(id))
                .expectError(ResourceNotFoundException.class)
                .verify();
    }

    @Test
    void shouldListCategoriesByOrg() {
        UUID orgId = UUID.randomUUID();
        VehicleCategoryEntity cat = VehicleCategoryEntity.builder().id(UUID.randomUUID()).name("Berline").build();
        when(categoryRepository.findAllByOrganizationIdOrSystem(orgId)).thenReturn(Flux.just(cat));

        StepVerifier.create(categoryUseCase.getByOrg(orgId))
                .expectNextCount(1)
                .verifyComplete();
    }

    @Test
    void shouldListAllCategories() {
        when(categoryRepository.findAll()).thenReturn(Flux.just(
                VehicleCategoryEntity.builder().id(UUID.randomUUID()).name("Sedan").build()));

        StepVerifier.create(categoryUseCase.getAllCategories())
                .expectNextCount(1)
                .verifyComplete();
    }

    @Test
    void shouldListCategoriesByAgency() {
        UUID agencyId = UUID.randomUUID();
        when(categoryRepository.findAllByAgencyIdOrSystem(agencyId)).thenReturn(Flux.just(
                VehicleCategoryEntity.builder().id(UUID.randomUUID()).name("Van").build()));

        StepVerifier.create(categoryUseCase.getByAgency(agencyId))
                .expectNextCount(1)
                .verifyComplete();
    }

    @Test
    void shouldUpdateCategory() {
        UUID id = UUID.randomUUID();
        VehicleCategoryEntity existing = VehicleCategoryEntity.builder()
                .id(id).organizationId(UUID.randomUUID()).name("Old").build();
        when(categoryRepository.findById(id)).thenReturn(Mono.just(existing));
        when(categoryRepository.save(any())).thenAnswer(inv -> Mono.just(inv.getArgument(0)));

        StepVerifier.create(categoryUseCase.update(id, new CategoryRequestDTO("New", "Desc")))
                .expectNextMatches(dto -> dto.name().equals("New"))
                .verifyComplete();
    }

    @Test
    void shouldRejectDeleteForSystemCategory() {
        UUID id = UUID.randomUUID();
        when(categoryRepository.findById(id)).thenReturn(Mono.just(
                VehicleCategoryEntity.builder().id(id).organizationId(null).name("System").build()));

        StepVerifier.create(categoryUseCase.delete(id))
                .expectError(ValidationException.class)
                .verify();
    }

    @Test
    void shouldDeleteOrganizationCategory() {
        UUID id = UUID.randomUUID();
        when(categoryRepository.findById(id)).thenReturn(Mono.just(
                VehicleCategoryEntity.builder().id(id).organizationId(UUID.randomUUID()).name("Custom").build()));
        when(categoryRepository.deleteById(id)).thenReturn(Mono.empty());

        StepVerifier.create(categoryUseCase.delete(id))
                .verifyComplete();
    }
}
