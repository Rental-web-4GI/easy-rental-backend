package com.yowyob.easyrental.modules.agency.domain.port.out;

import com.yowyob.easyrental.modules.agency.domain.AgencyEntity;
import java.util.UUID;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Outgoing port for agency persistence.
 */
public interface AgencyRepositoryPort {

    Mono<AgencyEntity> findById(UUID id);

    Mono<AgencyEntity> save(AgencyEntity agency);

    Mono<Void> deleteById(UUID id);

    Flux<AgencyEntity> findAll();

    Flux<AgencyEntity> findAllByOrganizationId(UUID organizationId);

    Mono<UUID> findOrgIdByAgencyId(UUID agencyId);

    Flux<AgencyEntity> searchAgencies(String keyword, String city);
}
