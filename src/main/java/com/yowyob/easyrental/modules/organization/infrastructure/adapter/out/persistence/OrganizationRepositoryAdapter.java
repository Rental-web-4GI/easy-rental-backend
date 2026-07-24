package com.yowyob.easyrental.modules.organization.infrastructure.adapter.out.persistence;

import com.yowyob.easyrental.modules.organization.domain.OrganizationEntity;
import com.yowyob.easyrental.modules.organization.domain.port.out.OrganizationRepositoryPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.UUID;
import java.time.LocalDateTime;

@Component
@RequiredArgsConstructor
public class OrganizationRepositoryAdapter implements OrganizationRepositoryPort {

    private final OrganizationRepository organizationRepository;

    @Override
    public Mono<OrganizationEntity> findById(UUID id) {
        return organizationRepository.findById(id);
    }

    @Override
    public Mono<OrganizationEntity> save(OrganizationEntity organization) {
        return organizationRepository.save(organization);
    }

    @Override
    public Flux<OrganizationEntity> findAll() {
        return organizationRepository.findAll();
    }

    @Override
    public Flux<OrganizationEntity> findAllBySubscriptionPlanId(UUID subscriptionPlanId) {
        return organizationRepository.findAllBySubscriptionPlanId(subscriptionPlanId);
    }

    @Override
    public Mono<OrganizationEntity> findByOwnerId(UUID ownerId) {
        return organizationRepository.findByOwnerId(ownerId);
    }

    @Override
    public Mono<OrganizationEntity> findByKernelOrganizationId(UUID kernelOrganizationId) {
        return organizationRepository.findByKernelOrganizationId(kernelOrganizationId);
    }

    @Override
    public Flux<OrganizationEntity> findAllExpiredBefore(LocalDateTime threshold) {
        return organizationRepository.findAllExpiredBefore(threshold);
    }

    @Override
    public Mono<Long> count() {
        return organizationRepository.count();
    }

    @Override
    public Mono<Long> countByAccountType(String accountType) {
        return organizationRepository.countByAccountType(accountType);
    }

    @Override
    public Mono<Long> countByGovernanceStatus(String governanceStatus) {
        return organizationRepository.countByGovernanceStatus(governanceStatus);
    }
}
