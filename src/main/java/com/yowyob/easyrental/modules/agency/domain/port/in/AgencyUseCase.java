package com.yowyob.easyrental.modules.agency.domain.port.in;

import com.yowyob.easyrental.modules.agency.dto.AgencyRequestDTO;
import com.yowyob.easyrental.modules.agency.dto.AgencyResponseDTO;
import java.util.UUID;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Incoming port for agency use cases.
 *
 * @author Easy Rental Team
 * @since 2026-06-03
 */
public interface AgencyUseCase {
    Mono<AgencyResponseDTO> createAgency(UUID orgId, AgencyRequestDTO request);
    Mono<Boolean> canAddResource(UUID orgId, String resourceType);
    Flux<AgencyResponseDTO> getAgenciesByOrg(UUID orgId);
    Flux<AgencyResponseDTO> getAllAgencies();
    Mono<AgencyResponseDTO> getAgency(UUID id);
    Flux<AgencyResponseDTO> searchAgencies(String keyword, String city);
    Mono<AgencyResponseDTO> updateAgency(UUID id, AgencyRequestDTO request);
    Mono<Void> deleteAgency(UUID id);
}
