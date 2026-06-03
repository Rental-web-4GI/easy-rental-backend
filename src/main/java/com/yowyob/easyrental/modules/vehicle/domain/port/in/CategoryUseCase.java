package com.yowyob.easyrental.modules.vehicle.domain.port.in;

import com.yowyob.easyrental.modules.vehicle.dto.CategoryRequestDTO;
import com.yowyob.easyrental.modules.vehicle.dto.CategoryResponseDTO;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.UUID;

/**
 * Incoming port for vehicle category use cases.
 *
 * @author Easy Rental Team
 * @since 2026-06-03
 */
public interface CategoryUseCase {

    Mono<CategoryResponseDTO> create(UUID orgId, CategoryRequestDTO request);

    Flux<CategoryResponseDTO> getByOrg(UUID orgId);

    Flux<CategoryResponseDTO> getAllCategories();

    Flux<CategoryResponseDTO> getByAgency(UUID agencyId);

    Mono<CategoryResponseDTO> getById(UUID id);

    Mono<Void> delete(UUID id);

    Mono<CategoryResponseDTO> update(UUID id, CategoryRequestDTO request);
}
