package com.yowyob.easyrental.modules.agency.infrastructure.adapter.out.persistence;

import com.yowyob.easyrental.modules.agency.domain.AgencyEntity;
import com.yowyob.easyrental.modules.agency.domain.port.out.AgencyRepositoryPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.UUID;

@Component
@RequiredArgsConstructor
public class AgencyRepositoryAdapter implements AgencyRepositoryPort {

    private final AgencyRepository agencyRepository;

    @Override
    public Mono<AgencyEntity> findById(UUID id) {
        return agencyRepository.findById(id);
    }

    @Override
    public Mono<AgencyEntity> save(AgencyEntity agency) {
        return agencyRepository.save(agency);
    }

    @Override
    public Mono<Void> deleteById(UUID id) {
        return agencyRepository.deleteById(id);
    }

    @Override
    public Flux<AgencyEntity> findAll() {
        return agencyRepository.findAll();
    }

    @Override
    public Flux<AgencyEntity> findAllByOrganizationId(UUID organizationId) {
        return agencyRepository.findAllByOrganizationId(organizationId);
    }

    @Override
    public Mono<UUID> findOrgIdByAgencyId(UUID agencyId) {
        return agencyRepository.findOrgIdByAgencyId(agencyId);
    }

    @Override
    public Flux<AgencyEntity> searchAgencies(String keyword, String city) {
        return agencyRepository.searchAgencies(keyword, city);
    }
}
